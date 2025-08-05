package com.scoresystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scoresystem.dto.ScoreSystemModels.ProjectDTO;
import com.scoresystem.dto.ScoreSystemModels.ScoreItemDTO;
import com.scoresystem.dto.ScoreSystemModels.TaskDTO;
import com.scoresystem.model.Project;
import com.scoresystem.model.Task;
import com.scoresystem.model.Score;
import com.scoresystem.model.ScoreItem;
import com.scoresystem.repository.ProjectRepository;
import com.scoresystem.repository.ScoreItemRepository;
import com.scoresystem.repository.ScoreItemRoleRepository;
import com.scoresystem.repository.TaskRepository;
import com.scoresystem.repository.ScoreRepository;
import com.scoresystem.service.ProjectService;
import com.scoresystem.service.TaskService;
import com.scoresystem.service.ScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.Set;
import com.scoresystem.model.User;
import com.scoresystem.repository.UserRepository;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

/**
 * 任务服务实现类
 */
@Service
@Transactional
public class TaskServiceImpl extends ServiceImpl<TaskRepository, Task> implements TaskService {

	@Autowired
	private TaskRepository taskRepository;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private ScoreRepository scoreRepository;

	@Autowired
	private ScoreItemRepository scoreItemRepository;

	@Autowired
	private ScoreItemRoleRepository scoreItemRoleRepository;

	@Autowired
	private UserRepository userRepository;

	/**
	 * 获取当前活动任务
	 */
	@Override
	public TaskDTO getActiveTask() {
		Task task = taskRepository.findActiveTask("active", new Date());
		if (task == null) {
			return null;
		}

		// 查询关联的专家和项目
		loadTaskRelations(task);

		return convertToDTO(task);
	}

	/**
	 * 获取所有任务
	 */
	@Override
	public List<TaskDTO> getAllTasks() {
		List<Task> tasks = taskRepository.selectList(null);
		return tasks.stream().map(task -> {
			// 查询关联的专家和项目
			loadTaskRelations(task);
			return convertToDTO(task);
		}).collect(Collectors.toList());
	}

	/**
	 * 根据ID获取任务
	 */
	@Override
	public TaskDTO getTaskById(Long taskId) {
		Task task = taskRepository.selectById(taskId);
		if (task == null) {
			return null;
		}

		// 查询关联的专家和项目
		loadTaskRelations(task);

		return convertToDTO(task);
	}

	/**
	 * 保存任务
	 */
	@Override
	public TaskDTO saveTask(TaskDTO taskDTO) {
		Task task;
		boolean isNew = taskDTO.getId() == null;

		if (isNew) {
			task = new Task();
		} else {
			task = taskRepository.selectById(taskDTO.getId());
			if (task == null) {
				return null;
			}
		}

		// 更新字段
		task.setTaskId(taskDTO.getTaskId());
		task.setCategory(taskDTO.getCategory());
		task.setTaskType(taskDTO.getTaskType());
		task.setScoreGroupType(taskDTO.getScoreGroupType());
		task.setStatus(taskDTO.getStatus());
		task.setStartTime(taskDTO.getStartTime());
		task.setEndTime(taskDTO.getEndTime());
		task.setSwitchMode(taskDTO.getSwitchMode()); // 新增：同步switchMode

		// 保存任务
		if (isNew) {
			taskRepository.insert(task);
		} else {
			taskRepository.updateById(task);
		}

		// 处理专家关系（先删除后插入）
		saveTaskExperts(task.getId(), taskDTO.getExperts());
		if (taskDTO.getProjects() != null && taskDTO.getProjects().size() > 0) {
			// 处理项目关系（先删除后插入）
			saveTaskProjects(task.getId(), taskDTO.getProjects());
		}
		if (taskDTO.getProjectIds() != null && taskDTO.getProjectIds().size() > 0) {
			// 处理项目关系（先删除后插入）
			saveTaskProjectsForProjectIds(task.getId(), taskDTO.getProjectIds());
		}
		// 查询关联的专家和项目
		loadTaskRelations(task);

		return convertToDTO(task);
	}

	/**
	 * 删除任务
	 */
	@Override
	public void deleteTask(Long taskId) {
		// 删除任务关系
		deleteTaskRelations(taskId);

		// 删除任务
		taskRepository.deleteById(taskId);
	}

	/**
	 * 获取用户相关的任务
	 */
	@Override
	public List<TaskDTO> getTasksByUser(String username) {
		List<Task> tasks = taskRepository.findByExpert(username);
		return tasks.stream().map(task -> {
			// 查询关联的专家和项目
			loadTaskRelations(task);
			return convertToDTO(task);
		}).collect(Collectors.toList());
	}

	/**
	 * 查询任务关联的专家和项目
	 */
	private void loadTaskRelations(Task task) {
		// 查询关联的专家
		List<String> experts = getTaskExperts(task.getId());
		task.setExperts(experts);

		// 查询关联的项目
		List<Project> projects = getTaskProjects(task.getId());
		task.setProjects(projects);
	}

	/**
	 * 查询任务关联的专家
	 */
	private List<String> getTaskExperts(Long taskId) {
		// 查询关联的专家
		return jdbcTemplate.queryForList("SELECT expert_username FROM task_experts WHERE task_id = ?", String.class,
				taskId);
	}

	/**
	 * 查询任务关联的项目
	 */
	private List<Project> getTaskProjects(Long taskId) {
		// 查询关联的项目
		return projectRepository.findByTaskIdOrderByDisplayOrderAsc(taskId);
	}

	/**
	 * 保存任务关联的专家
	 */
	private void saveTaskExperts(Long taskId, List<String> experts) {
		// 删除旧关联
		jdbcTemplate.update("DELETE FROM task_experts WHERE task_id = ?", taskId);

		// 添加新关联
		if (experts != null && !experts.isEmpty()) {
			List<Object[]> batchArgs = new ArrayList<>();
			for (String expert : experts) {
				batchArgs.add(new Object[] { taskId, expert });
			}
			jdbcTemplate.batchUpdate("INSERT INTO task_experts (task_id, expert_username) VALUES (?, ?)", batchArgs);
		}
	}

	/**
	 * 保存任务关联的项目
	 */
	private void saveTaskProjects(Long taskId, List<ProjectDTO> projectDTOs) {
		jdbcTemplate.update("DELETE FROM task_projects WHERE task_id = ?", taskId);

		if (projectDTOs != null && !projectDTOs.isEmpty()) {
			List<Object[]> batchArgs = new ArrayList<>();
			for (int i = 0; i < projectDTOs.size(); i++) {
				ProjectDTO projectDTO = projectDTOs.get(i);
				batchArgs.add(new Object[] { taskId, projectDTO.getId(), i, 0 }); // is_reviewed=0
			}
			jdbcTemplate.batchUpdate(
					"INSERT INTO task_projects (task_id, project_id, project_order, is_reviewed) VALUES (?, ?, ?, ?)",
					batchArgs);
		}
	}

	private void saveTaskProjectsForProjectIds(Long taskId, List<String> projectIds) {
		jdbcTemplate.update("DELETE FROM task_projects WHERE task_id = ?", taskId);

		if (projectIds != null && !projectIds.isEmpty()) {
			List<Object[]> batchArgs = new ArrayList<>();
			for (int i = 0; i < projectIds.size(); i++) {
				String projectId = projectIds.get(i);
				batchArgs.add(new Object[] { taskId, projectId, i, 0 }); // is_reviewed=0
			}
			jdbcTemplate.batchUpdate(
					"INSERT INTO task_projects (task_id, project_id, project_order, is_reviewed) VALUES (?, ?, ?, ?)",
					batchArgs);
		}
	}

	/**
	 * 删除任务关联
	 */
	private void deleteTaskRelations(Long taskId) {
		// 删除专家关联
		jdbcTemplate.update("DELETE FROM task_experts WHERE task_id = ?", taskId);

		// 删除项目关联
		jdbcTemplate.update("DELETE FROM task_projects WHERE task_id = ?", taskId);
	}

	/**
	 * 转换Task实体到TaskDTO
	 */
	private TaskDTO convertToDTO(Task task) {
		TaskDTO dto = new TaskDTO();
		dto.setId(task.getId());
		dto.setTaskId(task.getTaskId());
		dto.setCategory(task.getCategory());
		dto.setTaskType(task.getTaskType());
		dto.setScoreGroupType(task.getScoreGroupType());
		dto.setStatus(task.getStatus());
		dto.setStartTime(task.getStartTime());
		dto.setEndTime(task.getEndTime());
		dto.setExperts(task.getExperts());
		dto.setSwitchMode(task.getSwitchMode()); // 新增：同步switchMode

		// 处理项目
		if (task.getProjects() != null) {
			dto.setProjects(task.getProjects().stream().filter(project -> project != null) // 过滤掉null项目
					.map(project -> {
						ProjectDTO projectDTO = getProjectById(project.getId());
						return projectDTO != null ? projectDTO : new ProjectDTO(); // 防止空指针异常
					}).collect(Collectors.toList()));
		} else {
			dto.setProjects(new ArrayList<>()); // 设置空列表而不是null
		}

		return dto;
	}

	/**
	 * 启用评审任务
	 */
	@Override
	public TaskDTO enableTask(Long taskId) {
		Task task = taskRepository.selectById(taskId);
		if (task == null) {
			return null;
		}

		// 先将所有任务设置为非活动状态
		jdbcTemplate.update("UPDATE tasks SET status = 'inactive' WHERE status = 'active'");

		// 设置当前任务为活动状态
		task.setStatus("active");
		task.setStartTime(new Date());
		taskRepository.updateById(task);

		// 查询关联的专家和项目
		loadTaskRelations(task);

		return convertToDTO(task);
	}

	/**
	 * 完成评审任务
	 */
	@Override
	@Transactional
	public TaskDTO completeTask(Long taskId, String username) {
		Task task = taskRepository.selectById(taskId);
		if (task == null) {
			return null;
		}

		// 修复：先验证该专家是否确实完成了所有项目的评分
		boolean hasCompletedAllProjects = validateExpertCompletion(taskId, username);
		if (!hasCompletedAllProjects) {
			throw new IllegalStateException("专家 " + username + " 尚未完成所有项目的评分，无法标记为已完成");
		}

		// 记录当前专家完成状态
		jdbcTemplate.update(
				"UPDATE task_experts SET completed = 1, completion_time = ? WHERE task_id = ? AND expert_username = ?",
				new Object[] { new Date(), taskId, username });

		// 检查是否所有专家都已完成评审
		boolean allExpertsCompleted = checkAllExpertsCompleted(taskId);

		if (allExpertsCompleted) {
			// 只有当所有专家都完成时，才设置任务为完成状态
			task.setStatus("completed");
			task.setEndTime(new Date());
			taskRepository.updateById(task);
		}

		// 查询关联的专家和项目
		loadTaskRelations(task);

		return convertToDTO(task);
	}

	/**
	 * 重置评审任务
	 */
	@Override
	@Transactional
	public TaskDTO resetTask(Long taskId) {
		Task task = taskRepository.selectById(taskId);
		if (task == null) {
			throw new RuntimeException("任务不存在");
		}
		if (!"active".equals(task.getStatus())) {
			throw new RuntimeException("只有已启用的任务才能重置");
		}

		// 删除评分详情记录（如果存在score_details表）
		try {
			jdbcTemplate.update("DELETE FROM score_details WHERE score_id IN (SELECT id FROM scores WHERE task_id = ?)",
					taskId);
		} catch (Exception e) {
			System.out.println("score_details表可能不存在或为空: " + e.getMessage());
		}

		// 删除所有评分记录
		QueryWrapper<Score> scoreWrapper = new QueryWrapper<>();
		scoreWrapper.eq("task_id", taskId);
		scoreRepository.delete(scoreWrapper);

		// 重置项目评审状态
		jdbcTemplate.update("UPDATE task_projects SET is_reviewed = 0 WHERE task_id = ?", taskId);

		// 重置任务状态为待启用
		task.setStatus("pending");
		task.setUpdateTime(new Date());
		taskRepository.updateById(task);

		loadTaskRelations(task);
		return convertToDTO(task);
	}

	/**
	 * 调整任务项目顺序（仅手动切换模式且项目未评审时可用）
	 */
	@Override
	@Transactional
	public TaskDTO reorderTaskProjects(Long taskId, List<Long> projectIds) {
		Task task = taskRepository.selectById(taskId);
		if (task == null) {
			throw new RuntimeException("任务不存在");
		}

		// 检查任务是否为手动切换模式
		if (task.getTaskType() != 1 || task.getSwitchMode() != 2) {
			throw new RuntimeException("只有同步评审且手动切换模式的任务才能调整项目顺序");
		}

		// 检查任务状态
		if (!"active".equals(task.getStatus())) {
			throw new RuntimeException("只有已启用的任务才能调整项目顺序");
		}

		// 检查是否有项目已经开始评审
		List<Map<String, Object>> projectStatus = jdbcTemplate
				.queryForList("SELECT project_id, is_reviewed FROM task_projects WHERE task_id = ?", taskId);

		boolean hasReviewedProject = projectStatus.stream()
				.anyMatch(project -> ((Number) project.get("is_reviewed")).intValue() == 1);

		if (hasReviewedProject) {
			throw new RuntimeException("已有项目开始评审，无法调整项目顺序");
		}

		// 1. 查询未完成评审的项目ID集合
		List<Long> unreviewedProjectIds = jdbcTemplate.queryForList(
				"SELECT project_id FROM task_projects WHERE task_id = ? AND is_reviewed = 0　AND NOT EXISTS(SELECT 1 FROM scores WHERE scores.task_id = task_projects.task_id AND scores.project_id = task_projects.project_id) ORDER BY project_order",
				Long.class, taskId);
//项目总数
		int projectCount = this.getCount(taskId);

// 2. 校验前端传入的项目ID集合与未完成评审的项目ID集合一致（内容一致即可，顺序可变）
		if (unreviewedProjectIds.size() != projectIds.size() || !unreviewedProjectIds.containsAll(projectIds)
				|| !projectIds.containsAll(unreviewedProjectIds)) {
			throw new RuntimeException("未完成项目列表不匹配，无法调整顺序");
		}

// 3. 仅调整未完成项目的顺序，已完成项目顺序保持不变
		int startIndex = 0;
		for (Long projectId : projectIds) {
			if (unreviewedProjectIds.contains(projectId)) {
				// 按前端新顺序设置
				int newOrder = projectIds.indexOf(projectId) + projectCount - unreviewedProjectIds.size();
				jdbcTemplate.update("UPDATE task_projects SET project_order = ? WHERE task_id = ? AND project_id = ?",
						newOrder, taskId, projectId);
			} else {
				// 已完成项目顺序不变
				jdbcTemplate.update("UPDATE task_projects SET project_order = ? WHERE task_id = ? AND project_id = ?",
						startIndex, taskId, projectId);
				startIndex++;
			}
		}

		loadTaskRelations(task);
		return convertToDTO(task);
	}

	/**
	 * 获取任务项目顺序调整权限状态
	 */
	@Override
	public Map<String, Object> getReorderPermission(Long taskId) {
		Map<String, Object> permission = new HashMap<>();

		Task task = taskRepository.selectById(taskId);
		if (task == null) {
			permission.put("canReorder", false);
			permission.put("reason", "任务不存在");
			return permission;
		}

		// 检查任务类型和切换模式
		boolean isSyncTask = task.getTaskType() == 1;
		boolean isManualSwitch = task.getSwitchMode() == 2;
		boolean isActive = "active".equals(task.getStatus());

		if (!isSyncTask || !isManualSwitch) {
			permission.put("canReorder", false);
			permission.put("reason", "只有同步评审且手动切换模式的任务才能调整项目顺序");
			return permission;
		}

		if (!isActive) {
			permission.put("canReorder", false);
			permission.put("reason", "只有已启用的任务才能调整项目顺序");
			return permission;
		}

		// 检查是否有项目已经开始评审
		List<Map<String, Object>> projectStatus = jdbcTemplate
				.queryForList("SELECT project_id, is_reviewed FROM task_projects WHERE task_id = ?", taskId);

		boolean hasReviewedProject = projectStatus.stream()
				.anyMatch(project -> ((Number) project.get("is_reviewed")).intValue() == 1);

		if (hasReviewedProject) {
			permission.put("canReorder", false);
			permission.put("reason", "已有项目开始评审，无法调整项目顺序");
			return permission;
		}

		permission.put("canReorder", true);
		permission.put("reason", "可以调整项目顺序");
		return permission;
	}

	/**
	 * 检查任务的所有专家是否都已完成评审
	 */
	private boolean checkAllExpertsCompleted(Long taskId) {
		// 查询该任务的所有专家
		List<String> allExperts = getTaskExperts(taskId);

		if (allExperts.isEmpty()) {
			return false; // 没有专家，任务无法完成
		}

		// 查询已完成的专家数量
		Integer completedCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM task_experts WHERE task_id = ? AND completed = 1", Integer.class, taskId);

		// 如果已完成的专家数量等于总专家数量，则所有专家都已完成
		return completedCount != null && completedCount.equals(allExperts.size());
	}

	/**
	 * 检查任务完成状态
	 */
	@Override
	public Map<String, Object> checkTaskCompletionStatus(Long taskId) {
		Map<String, Object> status = new HashMap<>();

		// 查询该任务的所有专家
		List<String> allExperts = getTaskExperts(taskId);

		if (allExperts.isEmpty()) {
			status.put("totalExperts", 0);
			status.put("completedExperts", 0);
			status.put("completionPercentage", 0.0);
			status.put("isCompleted", false);
			status.put("completedExpertsList", new ArrayList<>());
			return status;
		}

		// 查询已完成的专家
		List<String> completedExperts = jdbcTemplate.queryForList(
				"SELECT expert_username FROM task_experts WHERE task_id = ? AND completed = 1", String.class, taskId);

		int totalExperts = allExperts.size();
		int completedCount = completedExperts.size();
		double completionPercentage = totalExperts > 0 ? (double) completedCount / totalExperts * 100 : 0.0;

		status.put("totalExperts", totalExperts);
		status.put("completedExperts", completedCount);
		status.put("completionPercentage", completionPercentage);
		status.put("isCompleted", completedCount >= totalExperts && totalExperts > 0);
		status.put("completedExpertsList", completedExperts);
		status.put("allExpertsList", allExperts);

		return status;
	}

	/**
	 * 更新任务切换模式
	 */
	@Override
	public TaskDTO updateTaskSwitchMode(Long taskId, Integer switchMode) {
		Task task = taskRepository.selectById(taskId);
		if (task == null) {
			return null;
		}

		// 验证切换模式值
		if (switchMode == null || (switchMode != 1 && switchMode != 2)) {
			throw new IllegalArgumentException("切换模式必须是1（自动切换）或2（手动切换）");
		}

		// 判断是否是从手动切换到自动
		boolean isManualToAuto = (task.getSwitchMode() == 2 && switchMode == 1);

		// 更新切换模式
		task.setSwitchMode(switchMode);
		taskRepository.updateById(task);

		// 如果是从手动切换到自动，处理未自动标记的项目
		if (isManualToAuto) {
			// 查询该任务下所有 isReviewed=0 的项目
			List<Map<String, Object>> projects = jdbcTemplate
					.queryForList("SELECT project_id FROM task_projects WHERE task_id = ? AND is_reviewed = 0", taskId);
			for (Map<String, Object> row : projects) {
				Long projectId = ((Number) row.get("project_id")).longValue();

				// 1. 查询该项目下所有应有的专家-评分项组合
				List<Map<String, Object>> requiredDetails = jdbcTemplate.queryForList(
						"SELECT username, score_item_id FROM task_experts_details WHERE task_id = ? AND project_id = ?",
						taskId, projectId);

				// 2. 查询该项目下所有已完成的评分明细（非草稿）
				List<Map<String, Object>> actualDetails = jdbcTemplate
						.queryForList(
								"SELECT s.user_id AS username, d.score_item_id " + "FROM scores s "
										+ "JOIN score_details d ON s.id = d.score_id "
										+ "WHERE s.task_id = ? AND s.project_id = ? AND s.is_draft = 0",
								taskId, projectId);

				// 3. 转为Set方便对比
				Set<String> requiredSet = requiredDetails.stream()
						.map(m -> m.get("username") + "_" + m.get("score_item_id")).collect(Collectors.toSet());
				Set<String> actualSet = actualDetails.stream()
						.map(m -> m.get("username") + "_" + m.get("score_item_id")).collect(Collectors.toSet());

				// 4. 如果全部完成，则标记为已评审
				if (!requiredSet.isEmpty() && actualSet.containsAll(requiredSet)) {
					markProjectReviewed(taskId, projectId);
				}
			}
		}

		// 查询关联的专家和项目
		loadTaskRelations(task);

		return convertToDTO(task);
	}

	// 新增：只查主表，不查关联
	public List<TaskDTO> getAllSimpleTasks(boolean includeProjectCount) {
		List<Task> tasks = taskRepository.selectList(null); // 只查tasks表
		return tasks.stream().map(task -> {
			TaskDTO dto = new TaskDTO();
			dto.setId(task.getId());
			dto.setTaskId(task.getTaskId());
			dto.setCategory(task.getCategory());
			dto.setStatus(task.getStatus());
			dto.setSwitchMode(task.getSwitchMode());
			if (includeProjectCount) {
				Integer completedCount = jdbcTemplate.queryForObject(
						"SELECT COUNT(*) FROM task_projects WHERE task_id = ? ", Integer.class, task.getId());
				dto.setProjectCount(task.getProjects().size());
			}
			// 其它字段不set
			return dto;
		}).collect(Collectors.toList());
	}

	@Override
	public List<Map<String, Object>> getTaskProjectProgressAndScores(Long taskId) {
		List<ProjectDTO> projects = getSimpleProjectsByTask(taskId);
		List<String> experts = getTaskExperts(taskId);
		int totalExperts = experts.size();

		List<Map<String, Object>> result = new ArrayList<>();
		for (ProjectDTO project : projects) {
			// 统计已评分专家数
			int completedExperts = (int) scoreRepository.findFinalScoresByProjectIdAndTaskId(project.getId(), taskId)
					.stream().map(Score::getUserId).distinct().count();
			// 计算项目总分
			Double totalScore = calculateProjectTotalScore(project.getId(), taskId);

			Map<String, Object> map = new HashMap<>();
			map.put("projectId", project.getId());
			map.put("projectName", project.getName());
			map.put("completedExperts", completedExperts);
			map.put("totalExperts", totalExperts);
			map.put("completionRate", totalExperts == 0 ? 0 : (completedExperts * 100.0 / totalExperts));
			map.put("totalScore", totalScore != null ? totalScore : 0.0);
			map.put("isReviewed", project.getIsReviewed());

			// 1. 查询已评分专家账号
			List<String> scoredExperts = jdbcTemplate.queryForList(
					"SELECT DISTINCT user_id FROM scores WHERE project_id = ? AND task_id = ? AND is_draft = 0",
					String.class, project.getId(), taskId);

			// 2. 查询专家真实姓名
			List<String> scoredExpertNames = new ArrayList<>();
			if (!scoredExperts.isEmpty()) {
				List<User> users = userRepository.findByUsernames(scoredExperts);
				Map<String, String> usernameToName = users.stream()
						.collect(Collectors.toMap(User::getUsername, User::getName));
				for (String username : scoredExperts) {
					scoredExpertNames.add(usernameToName.getOrDefault(username, username));
				}
			}
			map.put("scoredExpertNames", scoredExpertNames);

			result.add(map);
		}
		return result;
	}

	public Integer getProjectOrder(Long taskId, Long projectId) {
		Integer order = jdbcTemplate.queryForObject(
				"SELECT project_order FROM task_projects WHERE task_id = ? AND project_id = ?", Integer.class, taskId,
				projectId);
		return order;
	}

	public Long getNextProjectId(Long taskId, int currentOrder) {
		List<Long> ids = jdbcTemplate.queryForList(
				"SELECT project_id FROM task_projects WHERE task_id = ? AND project_order = ?", Long.class, taskId,
				currentOrder + 1);
		return ids.isEmpty() ? null : ids.get(0);
	}

	public List<Map<String, Object>> getTaskProjectsWithOrderAndStatus(Long taskId) {
		return jdbcTemplate.queryForList(
				"SELECT project_id, project_order, is_reviewed FROM task_projects WHERE task_id = ? ORDER BY project_order ASC",
				taskId);
	}

	@Override
	public void markProjectReviewed(Long taskId, Long projectId) {
		jdbcTemplate.update("UPDATE task_projects SET is_reviewed = 1 WHERE task_id = ? AND project_id = ?", taskId,
				projectId);
		// 检查该任务下是否所有项目都已评审
		Integer unreviewedCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM task_projects WHERE task_id = ? AND is_reviewed = 0", Integer.class, taskId);
		if (unreviewedCount != null && unreviewedCount == 0) {
			// 1. 更新任务状态为已完成
			jdbcTemplate.update("UPDATE tasks SET status = 'completed', end_time = ? WHERE id = ?", new Date(), taskId);
			// 2. 更新所有专家为已完成
			jdbcTemplate.update("UPDATE task_experts SET completed = 1, completion_time = ? WHERE task_id = ?",
					new Date(), taskId);
		}
	}

	private ProjectDTO getProjectById(Long projectId) {
		Project project = projectRepository.selectById(projectId);
		if (project == null) {
			return null;
		}

		// 查询关联的评分项
		List<ScoreItem> scoreItems = scoreItemRepository.findByProjectId(projectId);

		// 查询每个评分项的角色列表
		for (ScoreItem scoreItem : scoreItems) {
			List<String> roles = scoreItemRoleRepository.findRolesByScoreItemId(scoreItem.getId());
			scoreItem.setRoles(roles);
			if (roles != null && !roles.isEmpty()) {
				// 为了兼容旧代码，设置第一个角色作为主角色
				scoreItem.setRole(roles.get(0));
			}
		}

		project.setScoreItems(scoreItems);

		ProjectDTO dto = convertToDTO(project);

		if (project.getScoreItems() != null) {
			List<ScoreItemDTO> scoreItemDTOs = scoreItems.stream().map(this::convertToScoreItemDTO)
					.collect(Collectors.toList());
			Map<String, List<Map<String, Object>>> scoreGroups = new HashMap<>();
			for (String groupType : Arrays.asList("preliminary", "semifinal", "final")) {
				List<Map<String, Object>> items = scoreItemDTOs.stream()
						.filter(item -> groupType.equals(item.getGroupType())).map(item -> {
							Map<String, Object> map = new HashMap<>();
							map.put("name", item.getName());
							map.put("minScore", item.getMinScore());
							map.put("maxScore", item.getMaxScore());
							map.put("roles", item.getRoles());
							return map;
						}).collect(Collectors.toList());
				scoreGroups.put(groupType, items);
			}
			dto.setScoreGroups(scoreGroups);
		}

		return dto;
	}

	/**
	 * 转换Project实体到ProjectDTO
	 */
	private ProjectDTO convertToDTO(Project project) {
		ProjectDTO dto = new ProjectDTO();
		dto.setId(project.getId());
		dto.setName(project.getName());
		dto.setDescription(project.getDescription());
		dto.setStatus(project.getStatus());
		dto.setDisplayOrder(project.getDisplayOrder());
		dto.setCreateTime(project.getCreateTime());
		dto.setUpdateTime(project.getUpdateTime());
		dto.setUnit(project.getUnit());
		dto.setLeader(project.getLeader());

		// 评分项
		if (project.getScoreItems() != null) {
			List<ScoreItemDTO> scoreItemDTOs = project.getScoreItems().stream().map(this::convertToScoreItemDTO)
					.collect(Collectors.toList());
			dto.setScoreItems(scoreItemDTOs);

			// 组装scoreGroups为Map
			Map<String, List<Map<String, Object>>> scoreGroups = new HashMap<>();
			for (String groupType : Arrays.asList("preliminary", "semifinal", "final")) {
				List<Map<String, Object>> items = scoreItemDTOs.stream()
						.filter(item -> groupType.equals(item.getGroupType())).map(item -> {
							Map<String, Object> map = new HashMap<>();
							map.put("name", item.getName());
							map.put("minScore", item.getMinScore());
							map.put("maxScore", item.getMaxScore());
							map.put("roles", item.getRoles());
							return map;
						}).collect(Collectors.toList());
				scoreGroups.put(groupType, items);
			}
			dto.setScoreGroups(scoreGroups);
		}
		return dto;
	}

	/**
	 * 转换ScoreItem实体到ScoreItemDTO
	 */
	private ScoreItemDTO convertToScoreItemDTO(ScoreItem scoreItem) {
		ScoreItemDTO dto = new ScoreItemDTO();
		dto.setId(scoreItem.getId());
		dto.setProjectId(scoreItem.getProjectId());
		dto.setName(scoreItem.getName());
		dto.setDescription(scoreItem.getDescription());
		dto.setRole(scoreItem.getRole());
		dto.setWeight(scoreItem.getWeight());
		dto.setMinScore(scoreItem.getMinScore());
		dto.setMaxScore(scoreItem.getMaxScore());
		dto.setDisplayOrder(scoreItem.getDisplayOrder());
		dto.setGroupType(scoreItem.getGroupType());

		// 传递角色列表
		dto.setRoles(scoreItem.getRoles());

		return dto;
	}

	private List<ProjectDTO> getSimpleProjectsByTask(Long taskId) {
		List<Project> projects = projectRepository.findByTaskIdOrderByDisplayOrderAsc(taskId);
		return projects.stream().map(project -> {
			ProjectDTO dto = new ProjectDTO();
			dto.setId(project.getId());
			dto.setName(project.getName());
			dto.setStatus(project.getStatus());
			dto.setUnit(project.getUnit());
			dto.setLeader(project.getLeader());
			dto.setCreateTime(project.getCreateTime());
			dto.setIsReviewed(project.getIsReviewed());
			return dto;
		}).collect(Collectors.toList());
	}

	/**
	 * 计算项目总评分（指定任务）
	 */
	private Double calculateProjectTotalScore(Long projectId, Long taskId) {
		List<Score> finalScores = scoreRepository.findFinalScoresByProjectIdAndTaskId(projectId, taskId);

		if (finalScores.isEmpty()) {
			return 0.0;
		}

		// 计算总分（所有专家评分的总和）
		double sum = finalScores.stream().mapToDouble(Score::getTotalScore).sum();

		return sum;
	}

	/**
	 * 验证专家是否完成了所有项目的评分
	 */
	private boolean validateExpertCompletion(Long taskId, String username) {
		try {
			// 获取该任务下的所有项目
			List<Long> projectIds = jdbcTemplate.queryForList(
					"SELECT project_id FROM task_projects WHERE task_id = ? ORDER BY project_order ASC", Long.class,
					taskId);

			if (projectIds.isEmpty()) {
				return false;
			}

			// 检查每个项目是否都有该专家的评分记录
			for (Long projectId : projectIds) {
				Integer scoreCount = jdbcTemplate.queryForObject(
						"SELECT COUNT(*) FROM scores WHERE task_id = ? AND project_id = ? AND user_id = ? AND is_draft = 0",
						Integer.class, taskId, projectId, username);

				if (scoreCount == null || scoreCount == 0) {
					System.err.println("专家 " + username + " 尚未完成项目 " + projectId + " 的评分");
					return false;
				}
			}

			return true;
		} catch (Exception e) {
			System.err.println("验证专家完成状态时发生错误: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	private int getReviewedCount(Long taskId) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM task_projects WHERE task_id = ? AND is_reviewed = 1 ", Integer.class, taskId);
		return count != null ? count : 0;
	}

	private int getCount(Long taskId) {
		Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM task_projects WHERE task_id = ? ",
				Integer.class, taskId);
		return count != null ? count : 0;
	}
}
