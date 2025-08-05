package com.scoresystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scoresystem.dto.ScoreSystemModels.ScoreDTO;
import com.scoresystem.dto.ScoreSystemModels.ScoreRequest;
import com.scoresystem.model.Project;
import com.scoresystem.model.Score;
import com.scoresystem.model.ScoreItem;
import com.scoresystem.model.Task;
import com.scoresystem.model.User;
import com.scoresystem.repository.ProjectRepository;
import com.scoresystem.repository.ScoreItemRepository;
import com.scoresystem.repository.ScoreRepository;
import com.scoresystem.repository.TaskRepository;
import com.scoresystem.repository.UserRepository;
import com.scoresystem.service.ScoreService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 评分服务实现类
 */
@Service
@Transactional
@Profile("!test")
public class ScoreServiceImpl extends ServiceImpl<ScoreRepository, Score> implements ScoreService, DisposableBean {

	@Autowired
	private ScoreRepository scoreRepository;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ScoreItemRepository scoreItemRepository;

	@Autowired
	private TaskRepository taskRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	// 线程池，用于异步处理项目评审状态检查
	private final ExecutorService reviewStatusExecutor = Executors.newFixedThreadPool(5);
	
	// 定时任务线程池，用于批量处理评审状态检查
	private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(2);
	
	// 待检查的项目评审状态队列，使用ConcurrentHashMap避免并发问题
	private final ConcurrentHashMap<String, Boolean> pendingReviewChecks = new ConcurrentHashMap<>();
	
	// 初始化定时任务
	{
		// 每5秒检查一次待处理的项目评审状态
		scheduledExecutor.scheduleWithFixedDelay(this::processPendingReviewChecks, 5, 5, TimeUnit.SECONDS);
	}

	/**
	 * 保存评分
	 */
	@Override
	@Transactional
	public ScoreDTO saveScore(ScoreRequest scoreRequest) {
		// 查询项目和用户
		Project project = projectRepository.selectById(scoreRequest.getProjectId());
		User user = userRepository.findByUsername(scoreRequest.getUsername());

		if (project == null || user == null) {
			return null;
		}

		// 查找现有评分或创建新评分（需要同时匹配项目、任务和用户）
		List<Score> existingScores = scoreRepository.findByProjectIdAndTaskIdAndUsername(scoreRequest.getProjectId(),
				scoreRequest.getTaskId(), scoreRequest.getUsername());

		Score score;
		if (existingScores.isEmpty() || scoreRequest.getIsDraft()) {
			// 创建新评分
			score = new Score();
			score.setProjectId(scoreRequest.getProjectId());
			score.setTaskId(scoreRequest.getTaskId());
			score.setUserId(scoreRequest.getUsername());
			score.setCreateTime(new Date());
		} else {
			// 更新现有评分
			score = existingScores.get(0);
		}

		// 更新评分字段
		score.setComments(scoreRequest.getComments());
		score.setIsDraft(scoreRequest.getIsDraft());
		score.setUpdateTime(new Date());

		// 计算总分
		double totalScore = 0.0;
		if (scoreRequest.getScores() != null && !scoreRequest.getScores().isEmpty()) {
			// 先获取所有评分项
			List<ScoreItem> scoreItems = scoreItemRepository.findByProjectId(scoreRequest.getProjectId());
			Map<Long, ScoreItem> scoreItemMap = scoreItems.stream()
					.collect(Collectors.toMap(ScoreItem::getId, item -> item));

			// 计算加权总分
			double weightSum = 0.0;
			for (Map.Entry<Long, Integer> entry : scoreRequest.getScores().entrySet()) {
				ScoreItem item = scoreItemMap.get(entry.getKey());
				if (item != null) {
					double weight = (item.getWeight() != null) ? item.getWeight() : 1.0;
					totalScore += entry.getValue() * weight;
					weightSum += weight;
				}
			}

			if (weightSum > 0) {
				totalScore = totalScore / weightSum;
			}
		}

		score.setTotalScore(totalScore);

		// 保存评分
		if (score.getId() == null) {
			scoreRepository.insert(score);
		} else {
			scoreRepository.updateById(score);
		}

		// 保存评分详情
		saveScoreDetails(score.getId(), scoreRequest.getScores());

		// 查询关联信息并转换
		score.setProject(project);
		score.setUser(user);
		score.setScores(scoreRequest.getScores());

		// 记录需要检查的项目评审状态（在事务外部处理）
		boolean needCheckReviewStatus = !score.getIsDraft();
			Long taskId = scoreRequest.getTaskId();
			Long projectId = scoreRequest.getProjectId();
		
		// 返回DTO
		ScoreDTO result = convertToDTO(score);
		
		// 在事务外部检查项目评审状态，避免死锁
		if (needCheckReviewStatus) {
			// 使用新的事务或异步处理来避免死锁
			checkAndMarkProjectReviewedAsync(taskId, projectId);
		}
		
		return result;
	}

	/**
	 * 异步检查并标记项目评审状态，避免死锁
	 */
	private void checkAndMarkProjectReviewedAsync(Long taskId, Long projectId) {
		// 生成唯一键，用于去重
		String key = taskId + "_" + projectId;
		
		// 如果已经在待检查队列中，则跳过
		if (pendingReviewChecks.containsKey(key)) {
			return;
		}
		
		// 添加到待检查队列
		pendingReviewChecks.put(key, true);
		
		// 使用线程池异步处理，避免创建过多线程
		reviewStatusExecutor.submit(() -> {
			try {
				// 等待一小段时间确保主事务已提交
				Thread.sleep(300);
				
				// 在新的事务上下文中检查项目评审状态
				checkAndMarkProjectReviewed(taskId, projectId);
			} catch (Exception e) {
				System.err.println("异步检查项目评审状态时发生错误: " + e.getMessage());
				e.printStackTrace();
			} finally {
				// 处理完成后从队列中移除
				pendingReviewChecks.remove(key);
			}
		});
	}

	/**
	 * 检查并标记项目评审状态
	 */
	private void checkAndMarkProjectReviewed(Long taskId, Long projectId) {
		// 添加重试机制，最多重试3次
		int maxRetries = 3;
		int retryCount = 0;
		
		while (retryCount < maxRetries) {
			try {
				Task task = taskRepository.selectById(taskId);
				if (task == null) return;
				int taskType = task.getTaskType(); // 1=同步, 2=异步
				int switchMode = task.getSwitchMode(); // 1=自动, 2=手动

				if ((taskType == 1 && switchMode == 1) || taskType == 2) {
					// 只有自动切换或异步才自动标记
					if (isProjectReviewed(projectId, taskId)) {
						markProjectReviewed(taskId, projectId);
					}
				}
				// 否则（同步+手动），不自动标记，等管理员手动调用接口
				break; // 成功执行，跳出重试循环
			} catch (Exception e) {
				retryCount++;
				System.err.println("检查项目评审状态时发生错误 (重试 " + retryCount + "/" + maxRetries + "): " + e.getMessage());
				
				if (retryCount >= maxRetries) {
					System.err.println("达到最大重试次数，放弃检查项目评审状态");
					e.printStackTrace();
				} else {
					try {
						// 等待一段时间后重试，使用指数退避
						Thread.sleep(1000 * (1 << retryCount));
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			}
		}
	}

	/**
	 * 获取评分历史
	 */
	@Override
	public List<ScoreDTO> getScoreHistory(Long projectId, String username) {
		List<Score> scores = scoreRepository.findByProjectIdAndUsername(projectId, username);

		// 加载关联信息
		return scores.stream().map(score -> {
			loadScoreRelations(score);
			return convertToDTO(score);
		}).collect(Collectors.toList());
	}

	/**
	 * 获取评分历史（指定任务）
	 */
	@Override
	public List<ScoreDTO> getScoreHistory(Long projectId, Long taskId, String username) {
		List<Score> scores = scoreRepository.findByProjectIdAndTaskIdAndUsername(projectId, taskId, username);

		// 加载关联信息
		return scores.stream().map(score -> {
			loadScoreRelations(score);
			return convertToDTO(score);
		}).collect(Collectors.toList());
	}

	/**
	 * 获取项目所有评分
	 */
	@Override
	public List<ScoreDTO> getScoresByProject(Long projectId) {
		List<Score> scores = scoreRepository.findByProjectId(projectId);

		// 加载关联信息
		return scores.stream().map(score -> {
			loadScoreRelations(score);
			return convertToDTO(score);
		}).collect(Collectors.toList());
	}

	/**
	 * 获取项目所有评分（指定任务）
	 */
	@Override
	public List<ScoreDTO> getScoresByProject(Long projectId, Long taskId) {
		List<Score> scores = scoreRepository.findByProjectIdAndTaskId(projectId, taskId);

		// 加载关联信息
		return scores.stream().map(score -> {
			loadScoreRelations(score);
			return convertToDTO(score);
		}).collect(Collectors.toList());
	}

	/**
	 * 获取用户所有评分
	 */
	@Override
	public List<ScoreDTO> getScoresByUser(String username) {
		List<Score> scores = scoreRepository.findByUsername(username);

		// 加载关联信息
		return scores.stream().map(score -> {
			loadScoreRelations(score);
			return convertToDTO(score);
		}).collect(Collectors.toList());
	}

	/**
	 * 获取用户所有评分（指定任务）
	 */
	@Override
	public List<ScoreDTO> getScoresByUser(String username, Long taskId) {
		List<Score> scores = scoreRepository.findByUsernameAndTaskId(username, taskId);

		// 加载关联信息
		return scores.stream().map(score -> {
			loadScoreRelations(score);
			return convertToDTO(score);
		}).collect(Collectors.toList());
	}

	/**
	 * 计算项目总评分
	 */
	@Override
	public Double calculateProjectTotalScore(Long projectId) {
		List<Score> finalScores = scoreRepository.findFinalScoresByProjectId(projectId);

		if (finalScores.isEmpty()) {
			return 0.0;
		}

		// 计算平均分
		double sum = finalScores.stream().mapToDouble(Score::getTotalScore).sum();

		return sum / finalScores.size();
	}

	/**
	 * 计算项目总评分（指定任务）
	 */
	@Override
	public Double calculateProjectTotalScore(Long projectId, Long taskId) {
		List<Score> finalScores = scoreRepository.findFinalScoresByProjectIdAndTaskId(projectId, taskId);

		if (finalScores.isEmpty()) {
			return 0.0;
		}

		// 计算平均分
		double sum = finalScores.stream().mapToDouble(Score::getTotalScore).sum();

		return sum / finalScores.size();
	}

	/**
	 * 计算评分项总评分
	 */
	@Override
	public Double calculateScoreItemAverage(Long projectId, Long scoreItemId) {
		return scoreRepository.calculateAverageScoreByProjectIdAndScoreItemId(projectId, scoreItemId);
	}

	/**
	 * 计算评分项总评分（指定任务）
	 */
	@Override
	public Double calculateScoreItemAverage(Long projectId, Long taskId, Long scoreItemId) {
		return scoreRepository.calculateAverageScoreByProjectIdAndTaskIdAndScoreItemId(projectId, taskId, scoreItemId);
	}

	/**
	 * 获取项目评分统计
	 */
	@Override
	public Map<String, Object> getProjectScoreStatistics(Long projectId) {
		Map<String, Object> statistics = new HashMap<>();

		// 获取项目
		Project project = projectRepository.selectById(projectId);
		if (project == null) {
			statistics.put("totalScore", 0.0);
			statistics.put("scorerCount", 0);
			statistics.put("itemScores", new HashMap<>());
			return statistics;
		}

		// 获取项目评分项
		List<ScoreItem> scoreItems = scoreItemRepository.findByProjectId(projectId);
		if (scoreItems == null) {
			scoreItems = new ArrayList<>();
		}

		// 计算总体评分
		Double totalScore = calculateProjectTotalScore(projectId);
		statistics.put("totalScore", totalScore);

		// 计算各评分项平均分
		Map<Long, Double> itemScores = new HashMap<>();
		for (ScoreItem item : scoreItems) {
			Double avgScore = calculateScoreItemAverage(projectId, item.getId());
			itemScores.put(item.getId(), avgScore != null ? avgScore : 0.0);
		}
		statistics.put("itemScores", itemScores);

		// 获取评分人数
		List<Score> finalScores = scoreRepository.findFinalScoresByProjectId(projectId);
		statistics.put("scorerCount", finalScores != null ? finalScores.size() : 0);

		return statistics;
	}

	/**
	 * 获取项目评分统计（指定任务）
	 */
	@Override
	public Map<String, Object> getProjectScoreStatistics(Long projectId, Long taskId) {
		Map<String, Object> statistics = new HashMap<>();

		// 获取项目信息
		Project project = projectRepository.selectById(projectId);
		if (project == null) {
			return statistics;
		}

		statistics.put("projectId", projectId);
		statistics.put("projectName", project.getName());
		statistics.put("taskId", taskId);

		// 获取该任务下的所有评分
		List<Score> scores = scoreRepository.findByProjectIdAndTaskId(projectId, taskId);
		statistics.put("totalScores", scores.size());

		// 计算平均分
		if (!scores.isEmpty()) {
			double averageScore = scores.stream().filter(score -> !score.getIsDraft() && score.getTotalScore() != null)
					.mapToDouble(Score::getTotalScore).average().orElse(0.0);
			statistics.put("averageScore", averageScore);
		} else {
			statistics.put("averageScore", 0.0);
		}

		// 统计草稿和最终评分数量
		long draftCount = scores.stream().filter(Score::getIsDraft).count();
		long finalCount = scores.size() - draftCount;
		statistics.put("draftCount", draftCount);
		statistics.put("finalCount", finalCount);

		// 获取评分项统计
		List<ScoreItem> scoreItems = scoreItemRepository.findByProjectId(projectId);
		List<Map<String, Object>> itemStats = new ArrayList<>();

		for (ScoreItem item : scoreItems) {
			Map<String, Object> itemStat = new HashMap<>();
			itemStat.put("scoreItemId", item.getId());
			itemStat.put("scoreItemName", item.getName());
			itemStat.put("weight", item.getWeight());

			// 计算该评分项的平均分
			Double avgScore = scoreRepository.calculateAverageScoreByProjectIdAndTaskIdAndScoreItemId(projectId, taskId,
					item.getId());
			itemStat.put("averageScore", avgScore != null ? avgScore : 0.0);

			itemStats.add(itemStat);
		}

		statistics.put("scoreItems", itemStats);

		return statistics;
	}

	/**
	 * 加载评分关联信息
	 */
	private void loadScoreRelations(Score score) {
		// 加载项目
		Project project = projectRepository.selectById(score.getProjectId());
		score.setProject(project);

		// 加载用户
		User user = userRepository.findByUsername(score.getUserId());
		score.setUser(user);

		// 加载评分详情
		Map<Long, Integer> scoreDetails = getScoreDetails(score.getId());
		score.setScores(scoreDetails);
	}

	/**
	 * 获取评分详情
	 */
	private Map<Long, Integer> getScoreDetails(Long scoreId) {
		return jdbcTemplate.query("SELECT score_item_id, score_value FROM score_details WHERE score_id = ?",
				new Object[] { scoreId }, rs -> {
					Map<Long, Integer> details = new HashMap<>();
					while (rs.next()) {
						details.put(rs.getLong("score_item_id"), rs.getInt("score_value"));
					}
					return details;
				});
	}

	/**
	 * 保存评分详情
	 */
	private void saveScoreDetails(Long scoreId, Map<Long, Integer> scores) {
		// 删除旧评分详情
		jdbcTemplate.update("DELETE FROM score_details WHERE score_id = ?", scoreId);

		// 添加新评分详情
		if (scores != null && !scores.isEmpty()) {
			for (Map.Entry<Long, Integer> entry : scores.entrySet()) {
				jdbcTemplate.update("INSERT INTO score_details (score_id, score_item_id, score_value) VALUES (?, ?, ?)",
						new Object[] { scoreId, entry.getKey(), entry.getValue() });
			}
		}
	}

	/**
	 * 转换Score实体到ScoreDTO
	 */
	private ScoreDTO convertToDTO(Score score) {
		ScoreDTO dto = new ScoreDTO();
		dto.setId(score.getId());
		dto.setProjectId(score.getProjectId());
		dto.setUsername(score.getUserId());
		dto.setTotalScore(score.getTotalScore());
		dto.setComments(score.getComments());
		dto.setCreateTime(score.getCreateTime());
		dto.setUpdateTime(score.getUpdateTime());
		dto.setIsDraft(score.getIsDraft());
		dto.setScores(score.getScores());
		dto.setTaskId(score.getTaskId());

		// 设置项目名称
		if (score.getProject() != null) {
			dto.setProjectName(score.getProject().getName());
		}

		// 设置用户名称
		if (score.getUser() != null) {
			dto.setUserFullName(score.getUser().getName());
		}

		return dto;
	}

	/**
	 * 获取所有评分记录
	 */
	@Override
	public List<ScoreDTO> getAllScores() {
		List<Score> scores = scoreRepository.selectList(null);
		return scores.stream().map(this::convertToDTO).collect(Collectors.toList());
	}

	/**
	 * 获取所有评分记录（指定任务）
	 */
	@Override
	public List<ScoreDTO> getAllScores(Long taskId) {
		List<Score> scores = scoreRepository.findByTaskId(taskId);

		// 加载关联信息
		return scores.stream().map(score -> {
			loadScoreRelations(score);
			return convertToDTO(score);
		}).collect(Collectors.toList());
	}

	/**
	 * 按任务获取评分记录
	 */
	@Override
	public List<ScoreDTO> getScoresByTask(Long taskId) {
		List<Score> scores = scoreRepository.findByTaskId(taskId);

		// 加载关联信息
		return scores.stream().map(score -> {
			loadScoreRelations(score);
			return convertToDTO(score);
		}).collect(Collectors.toList());
	}

	private boolean isProjectReviewed(Long projectId, Long taskId) {
		try {
			// 1. 获取该任务下该项目的所有专家
			List<String> assignedExperts = jdbcTemplate.queryForList(
					"SELECT username FROM task_experts_details WHERE task_id = ? AND project_id = ?", 
					String.class, taskId, projectId);
			
			if (assignedExperts.isEmpty()) {
				System.out.println("项目 " + projectId + " 在任务 " + taskId + " 中没有分配专家");
				return false;
			}
			
			// 2. 获取已完成评分的专家（非草稿）
			List<String> completedExperts = jdbcTemplate.queryForList(
					"SELECT DISTINCT user_id FROM scores WHERE project_id = ? AND task_id = ? AND is_draft = 0", 
					String.class, projectId, taskId);
			
			// 3. 检查是否所有专家都已完成评分
			boolean allExpertsCompleted = assignedExperts.stream()
					.allMatch(expert -> completedExperts.contains(expert));
			
			// 添加调试信息
			System.out.println("项目评审状态检查 - 项目ID: " + projectId + ", 任务ID: " + taskId + 
					", 分配专家数: " + assignedExperts.size() + 
					", 已完成专家数: " + completedExperts.size() + 
					", 是否全部完成: " + allExpertsCompleted);
			
			return allExpertsCompleted;
			
		} catch (Exception e) {
			// 记录详细错误信息
			System.err.println("检查项目评审状态时SQL查询失败: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	private void markProjectReviewed(Long taskId, Long projectId) {
		jdbcTemplate.update("UPDATE task_projects SET is_reviewed = 1 WHERE task_id = ? AND project_id = ?", taskId,
				projectId);
		// 检查该任务下是否所有项目都已评审
		Integer unreviewedCount = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM task_projects WHERE task_id = ? AND is_reviewed = 0",
			Integer.class, taskId
		);
		if (unreviewedCount != null && unreviewedCount == 0) {
			// 1. 更新任务状态为已完成
			jdbcTemplate.update(
				"UPDATE tasks SET status = 'completed', end_time = ? WHERE id = ?",
				new Date(), taskId
			);
			// 2. 更新所有专家为已完成
			jdbcTemplate.update(
				"UPDATE task_experts SET completed = 1, completion_time = ? WHERE task_id = ?",
				new Date(), taskId
			);
		}		
	}

	/**
	 * 批量处理待检查的项目评审状态
	 */
	private void processPendingReviewChecks() {
		try {
			// 获取所有待检查的项目
			Set<String> keys = pendingReviewChecks.keySet();
			if (keys.isEmpty()) {
				return;
			}
			
			System.out.println("批量处理 " + keys.size() + " 个待检查的项目评审状态");
			
			// 批量处理每个项目
			for (String key : keys) {
				String[] parts = key.split("_");
				if (parts.length == 2) {
					Long taskId = Long.parseLong(parts[0]);
					Long projectId = Long.parseLong(parts[1]);
					
					// 使用线程池处理单个项目
					reviewStatusExecutor.submit(() -> {
						try {
							checkAndMarkProjectReviewed(taskId, projectId);
						} catch (Exception e) {
							System.err.println("批量处理项目评审状态时发生错误: " + e.getMessage());
							e.printStackTrace();
						} finally {
							// 处理完成后从队列中移除
							pendingReviewChecks.remove(key);
						}
					});
				}
			}
		} catch (Exception e) {
			System.err.println("批量处理待检查项目评审状态时发生错误: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * 实现DisposableBean接口的destroy方法
	 * 确保线程池在Bean销毁时正确关闭
	 */
	@Override
	public void destroy() throws Exception {
		shutdownThreadPools();
	}

	/**
	 * 关闭线程池的通用方法
	 */
	private void shutdownThreadPools() {
		try {
			System.out.println("正在关闭评分服务线程池...");
			
			// 关闭定时任务线程池
			if (scheduledExecutor != null && !scheduledExecutor.isShutdown()) {
				System.out.println("正在关闭定时任务线程池...");
				scheduledExecutor.shutdown();
				if (!scheduledExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
					System.out.println("定时任务线程池未能在10秒内关闭，强制关闭...");
					scheduledExecutor.shutdownNow();
				}
				System.out.println("定时任务线程池已关闭");
			}
			
			// 关闭评审状态检查线程池
			if (reviewStatusExecutor != null && !reviewStatusExecutor.isShutdown()) {
				System.out.println("正在关闭评审状态检查线程池...");
				reviewStatusExecutor.shutdown();
				if (!reviewStatusExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
					System.out.println("评审状态检查线程池未能在10秒内关闭，强制关闭...");
					reviewStatusExecutor.shutdownNow();
				}
				System.out.println("评审状态检查线程池已关闭");
			}
			
			// 清空待检查队列
			pendingReviewChecks.clear();
			System.out.println("评分服务线程池已完全关闭");
		} catch (Exception e) {
			System.err.println("关闭评分服务线程池时发生错误: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
