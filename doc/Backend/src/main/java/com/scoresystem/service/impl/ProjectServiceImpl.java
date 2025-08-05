package com.scoresystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scoresystem.dto.ScoreSystemModels.ProjectDTO;
import com.scoresystem.dto.ScoreSystemModels.ScoreItemDTO;
import com.scoresystem.model.Project;
import com.scoresystem.model.ScoreItem;
import com.scoresystem.model.ScoreItemRole;
import com.scoresystem.model.User;
import com.scoresystem.repository.ProjectRepository;
import com.scoresystem.repository.ScoreItemRepository;
import com.scoresystem.repository.ScoreItemRoleRepository;
import com.scoresystem.repository.ScoreRepository;
import com.scoresystem.repository.UserRepository;
import com.scoresystem.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.scoresystem.model.Score;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.Set;

/**
 * 项目服务实现类
 */
@Service
@Transactional
@Profile("!test")
public class ProjectServiceImpl extends ServiceImpl<ProjectRepository, Project> implements ProjectService {

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ScoreItemRepository scoreItemRepository;

	@Autowired
	private ScoreItemRoleRepository scoreItemRoleRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ScoreRepository scoreRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/**
	 * 获取所有项目
	 */
	@Override
	public List<ProjectDTO> getAllProjects() {
		List<Project> projects = projectRepository.findAllByOrderByDisplayOrderAsc();
		List<ProjectDTO> projectDTOs = new ArrayList<>();
		for (Project project : projects) {
			// 查询并设置评分项
			List<ScoreItem> scoreItems = scoreItemRepository.findByProjectId(project.getId());
			for (ScoreItem scoreItem : scoreItems) {
				List<String> roles = scoreItemRoleRepository.findRolesByScoreItemId(scoreItem.getId());
				scoreItem.setRoles(roles);
				if (roles != null && !roles.isEmpty()) {
					scoreItem.setRole(roles.get(0));
				}
			}
			project.setScoreItems(scoreItems);
			ProjectDTO dto = convertToDTO(project);

			// 组装scoreGroups
			if (scoreItems != null) {
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
			projectDTOs.add(dto);
		}
		return projectDTOs;
	}

	/**
	 * 根据ID获取项目
	 */
	@Override
	public ProjectDTO getProjectById(Long projectId) {
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
	 * 保存项目
	 */
	@Override
	public ProjectDTO saveProject(ProjectDTO projectDTO) {
		Project project;
		boolean isNew = projectDTO.getId() == null;

		if (isNew) {
			project = new Project();
			project.setCreateTime(new Date());
		} else {
			project = projectRepository.selectById(projectDTO.getId());
			if (project == null) {
				return null;
			}
		}

		// 更新字段
		project.setName(projectDTO.getName());
		project.setDescription(projectDTO.getDescription());
		project.setStatus(projectDTO.getStatus() != null ? projectDTO.getStatus() : "draft");
		project.setDisplayOrder(projectDTO.getDisplayOrder() != null ? projectDTO.getDisplayOrder() : 0);
		project.setUpdateTime(new Date());
		project.setUnit(projectDTO.getUnit());
		project.setLeader(projectDTO.getLeader());
		project.setIndustry(projectDTO.getIndustry());

		// 保存项目
		if (isNew) {
			projectRepository.insert(project);
		} else {
			projectRepository.updateById(project);
		}

		// 处理评分项数据
		if (projectDTO.getScoreGroups() != null) {
			// 删除旧的评分项
			if (!isNew) {
				QueryWrapper<ScoreItem> scoreItemWrapper = new QueryWrapper<>();
				scoreItemWrapper.eq("project_id", project.getId());
				List<ScoreItem> oldScoreItems = scoreItemRepository.selectList(scoreItemWrapper);

				// 删除每个评分项的角色关联
				for (ScoreItem scoreItem : oldScoreItems) {
					scoreItemRoleRepository.deleteByScoreItemId(scoreItem.getId());
				}

				// 删除评分详情记录（如果存在score_details表）
				for (ScoreItem scoreItem : oldScoreItems) {
					try {
						jdbcTemplate.update("DELETE FROM score_details WHERE score_item_id = ?", scoreItem.getId());
					} catch (Exception e) {
						// 如果表不存在或没有数据，忽略错误
						System.out.println("score_details表可能不存在或为空: " + e.getMessage());
					}
				}

				// 删除旧的评分项
				scoreItemRepository.delete(scoreItemWrapper);
			}

			// 保存新的评分项
			int displayOrder = 1;
			for (String groupType : Arrays.asList("preliminary", "semifinal", "final")) {
				List<Map<String, Object>> groupItems = projectDTO.getScoreGroups().get(groupType);
				if (groupItems != null) {
					for (Map<String, Object> itemData : groupItems) {
						ScoreItem scoreItem = new ScoreItem();
						scoreItem.setProjectId(project.getId());
						scoreItem.setName((String) itemData.get("name"));
						scoreItem.setGroupType(groupType);
						scoreItem.setDisplayOrder(displayOrder++);

						// 设置最小值和最大值
						Object minScoreObj = itemData.get("minScore");
						if (minScoreObj != null) {
							if (minScoreObj instanceof Integer) {
								scoreItem.setMinScore((Integer) minScoreObj);
							} else if (minScoreObj instanceof String) {
								scoreItem.setMinScore(Integer.parseInt((String) minScoreObj));
							}
						} else {
							scoreItem.setMinScore(0); // 默认最小值
						}

						Object maxScoreObj = itemData.get("maxScore");
						if (maxScoreObj != null) {
							if (maxScoreObj instanceof Integer) {
								scoreItem.setMaxScore((Integer) maxScoreObj);
							} else if (maxScoreObj instanceof String) {
								scoreItem.setMaxScore(Integer.parseInt((String) maxScoreObj));
							}
						} else {
							scoreItem.setMaxScore(100); // 默认最大值
						}

						// 保存评分项
						scoreItemRepository.insert(scoreItem);

						// 保存角色关联
						@SuppressWarnings("unchecked")
						List<String> roles = (List<String>) itemData.get("roles");
						if (roles != null && !roles.isEmpty()) {
							for (String role : roles) {
								ScoreItemRole scoreItemRole = new ScoreItemRole();
								scoreItemRole.setScoreItemId(scoreItem.getId());
								scoreItemRole.setRole(role);
								scoreItemRoleRepository.insert(scoreItemRole);
							}
						}
					}
				}
			}
		}

		return convertToDTO(project);
	}

	/**
	 * 删除项目
	 */
	@Override
	public void deleteProject(Long projectId) {
		// 获取项目关联的所有评分项
		QueryWrapper<ScoreItem> scoreItemWrapper = new QueryWrapper<>();
		scoreItemWrapper.eq("project_id", projectId);
		List<ScoreItem> scoreItems = scoreItemRepository.selectList(scoreItemWrapper);

		// 删除每个评分项的角色关联
		for (ScoreItem scoreItem : scoreItems) {
			scoreItemRoleRepository.deleteByScoreItemId(scoreItem.getId());
		}

		// 删除评分详情记录（如果存在score_details表）
		for (ScoreItem scoreItem : scoreItems) {
			try {
				jdbcTemplate.update("DELETE FROM score_details WHERE score_item_id = ?", scoreItem.getId());
			} catch (Exception e) {
				// 如果表不存在或没有数据，忽略错误
				System.out.println("score_details表可能不存在或为空: " + e.getMessage());
			}
		}

		// 删除项目关联的评分项
		scoreItemRepository.delete(scoreItemWrapper);

		// 删除项目关联的评分
		QueryWrapper<Score> scoreWrapper = new QueryWrapper<>();
		scoreWrapper.eq("project_id", projectId);
		scoreRepository.delete(scoreWrapper);

		// 删除项目关联的任务关系
		jdbcTemplate.update("DELETE FROM task_projects WHERE project_id = ?", projectId);

		// 删除项目
		projectRepository.deleteById(projectId);
	}

	/**
	 * 根据任务ID获取项目列表
	 */
	@Override
	public List<ProjectDTO> getProjectsByTask(Long taskId) {
		List<Project> projects = projectRepository.findByTaskIdOrderByDisplayOrderAsc(taskId);
		List<ProjectDTO> projectDTOs = new ArrayList<>();
		for (Project project : projects) {
			// 新增：查找并设置评分项
			List<ScoreItem> scoreItems = scoreItemRepository.findByProjectId(project.getId());
			for (ScoreItem scoreItem : scoreItems) {
				List<String> roles = scoreItemRoleRepository.findRolesByScoreItemId(scoreItem.getId());
				scoreItem.setRoles(roles);
				if (roles != null && !roles.isEmpty()) {
					scoreItem.setRole(roles.get(0));
				}
			}
			project.setScoreItems(scoreItems);
			projectDTOs.add(convertToDTO(project));
		}
		return projectDTOs;
	}

	/**
	 * 根据用户角色获取评分项 只返回用户角色有权限查看的评分项，确保安全性
	 */
	@Override
	public List<ScoreItemDTO> getScoreItemsByUserRole(Long projectId, String username) {
		// 查询用户
		User user = userRepository.findByUsername(username);
		if (user == null) {
			return new ArrayList<>();
		}
		// 防止空指针异常
		String role = user.getRole() != null ? user.getRole().toUpperCase() : "USER";
		// 查询与用户角色相关的评分项
		List<ScoreItem> scoreItems = scoreItemRepository.findByProjectIdAndRole(projectId, role);
		// 严格按角色过滤，如果用户角色没有对应的评分项，返回空列表
		if (scoreItems == null || scoreItems.isEmpty()) {
			return new ArrayList<>();
		}
		// 为每个评分项设置角色信息
		for (ScoreItem scoreItem : scoreItems) {
			List<String> roles = scoreItemRoleRepository.findRolesByScoreItemId(scoreItem.getId());
			scoreItem.setRoles(roles);
			scoreItem.setRole(role); // 设置当前用户的角色
		}
		return scoreItems.stream().map(this::convertToScoreItemDTO).collect(Collectors.toList());
	}

	/**
	 * 根据项目ID获取评分项
	 */
	@Override
	public List<ScoreItemDTO> getScoreItemsByProjectId(Long projectId) {
		List<ScoreItem> scoreItems = scoreItemRepository.findByProjectId(projectId);

		if (scoreItems == null) {
			return new ArrayList<>();
		}

		// 为每个评分项设置角色信息
		for (ScoreItem scoreItem : scoreItems) {
			List<String> roles = scoreItemRoleRepository.findRolesByScoreItemId(scoreItem.getId());
			scoreItem.setRoles(roles);
			if (roles != null && !roles.isEmpty()) {
				scoreItem.setRole(roles.get(0));
			}
		}

		return scoreItems.stream().map(this::convertToScoreItemDTO).collect(Collectors.toList());
	}

	/**
	 * 批量更新项目状态
	 */
	@Override
	public void batchUpdateStatus(List<Long> projectIds, String status) {
		if (projectIds == null || projectIds.isEmpty() || status == null) {
			return;
		}

		for (Long projectId : projectIds) {
			Project project = projectRepository.selectById(projectId);
			if (project != null) {
				project.setStatus(status);
				project.setUpdateTime(new Date());
				projectRepository.updateById(project);
			}
		}
	}

	/**
	 * 批量删除项目
	 */
	@Override
	public void batchDelete(List<Long> projectIds) {
		if (projectIds == null || projectIds.isEmpty()) {
			return;
		}

		for (Long projectId : projectIds) {
			// 获取项目关联的所有评分项
			QueryWrapper<ScoreItem> scoreItemWrapper = new QueryWrapper<>();
			scoreItemWrapper.eq("project_id", projectId);
			List<ScoreItem> scoreItems = scoreItemRepository.selectList(scoreItemWrapper);

			// 删除每个评分项的角色关联
			for (ScoreItem scoreItem : scoreItems) {
				scoreItemRoleRepository.deleteByScoreItemId(scoreItem.getId());
			}

			// 删除评分详情记录（如果存在score_details表）
			for (ScoreItem scoreItem : scoreItems) {
				try {
					jdbcTemplate.update("DELETE FROM score_details WHERE score_item_id = ?", scoreItem.getId());
				} catch (Exception e) {
					// 如果表不存在或没有数据，忽略错误
					System.out.println("score_details表可能不存在或为空: " + e.getMessage());
				}
			}

			// 删除项目关联的评分项
			scoreItemRepository.delete(scoreItemWrapper);

			// 删除项目关联的评分
			QueryWrapper<Score> scoreWrapper = new QueryWrapper<>();
			scoreWrapper.eq("project_id", projectId);
			scoreRepository.delete(scoreWrapper);

			// 删除项目关联的任务关系
			jdbcTemplate.update("DELETE FROM task_projects WHERE project_id = ?", projectId);

			// 删除项目
			projectRepository.deleteById(projectId);
		}
	}

	/**
	 * 更新项目顺序
	 */
	@Override
	public void updateOrder(List<Long> projectIds) {
		if (projectIds == null || projectIds.isEmpty()) {
			return;
		}

		for (int i = 0; i < projectIds.size(); i++) {
			Long projectId = projectIds.get(i);
			Project project = projectRepository.selectById(projectId);
			if (project != null) {
				project.setDisplayOrder(i + 1);
				project.setUpdateTime(new Date());
				projectRepository.updateById(project);
			}
		}
	}

	/**
	 * 获取项目评分进度
	 */
	@Override
	public Map<String, Object> getProjectProgress(Long projectId) {
		Map<String, Object> progress = new HashMap<>();

		// 获取项目
		Project project = projectRepository.selectById(projectId);
		if (project == null) {
			progress.put("totalExperts", 0);
			progress.put("completedExperts", 0);
			progress.put("completionPercentage", 0.0);
			return progress;
		}

		// 获取项目关联的任务
		List<Map<String, Object>> taskProjects = jdbcTemplate
				.queryForList("SELECT task_id FROM task_projects WHERE project_id = ?", projectId);

		if (taskProjects.isEmpty()) {
			progress.put("totalExperts", 0);
			progress.put("completedExperts", 0);
			progress.put("completionPercentage", 0.0);
			return progress;
		}

		Long taskId = (Long) taskProjects.get(0).get("task_id");

		// 获取任务关联的专家
		List<Map<String, Object>> taskExperts = jdbcTemplate
				.queryForList("SELECT expert_username FROM task_experts WHERE task_id = ?", taskId);

		int totalExperts = taskExperts.size();
		int completedScores = 0;

		// 计算已完成评分的专家数量和收集专家列表
		List<String> scoredExperts = new ArrayList<>();
		for (Map<String, Object> expert : taskExperts) {
			String username = (String) expert.get("expert_username");
			List<Map<String, Object>> scores = jdbcTemplate.queryForList(
					"SELECT * FROM scores WHERE project_id = ? AND user_id = ? AND is_draft = 0", projectId, username);

			if (!scores.isEmpty()) {
				completedScores++;
				scoredExperts.add(username);
			}
		}

		int total = totalExperts;
		int completed = completedScores;
		double percentage = total > 0 ? (double) completed / total * 100 : 0.0;

		progress.put("totalExperts", total);
		progress.put("completedExperts", completed);
		progress.put("completionPercentage", percentage);
		progress.put("scoredExperts", scoredExperts);
		progress.put("completed", completed >= total && total > 0);

		return progress;
	}

	/**
	 * 获取项目评分进度（指定任务）
	 */
	@Override
	public Map<String, Object> getProjectProgress(Long projectId, Long taskId) {
		Map<String, Object> progress = new HashMap<>();
		// 获取项目
		Project project = projectRepository.selectById(projectId);
		if (project == null) {
			progress.put("totalExperts", 0);
			progress.put("completedExperts", 0);
			progress.put("completionPercentage", 0.0);
			progress.put("completed", false);
			return progress;
		}
		// 1. 获取所有应评分(score_item_id, username)组合
		List<Map<String, Object>> requiredPairs = jdbcTemplate.queryForList(
				"SELECT score_item_id, username FROM task_experts_details WHERE task_id = ? AND project_id = ?", taskId,
				projectId);
		Set<String> requiredSet = requiredPairs.stream()
				.map(row -> row.get("score_item_id") + ":" + row.get("username")).collect(Collectors.toSet());
		// 2. 获取所有已评分(score_item_id, user_id)组合
		List<Map<String, Object>> scoredPairs = jdbcTemplate.queryForList(
				"SELECT sd.score_item_id, s.user_id FROM score_details sd " + "JOIN scores s ON sd.score_id = s.id "
						+ "WHERE s.project_id = ? AND s.task_id = ? AND s.is_draft = 0",
				projectId, taskId);
		Set<String> scoredSet = scoredPairs.stream().map(row -> row.get("score_item_id") + ":" + row.get("user_id"))
				.collect(Collectors.toSet());
		// 3. 判断是否全部已评分
		boolean completed = !requiredSet.isEmpty() && scoredSet.containsAll(requiredSet);
		progress.put("completed", completed);
		// 统计进度
		progress.put("totalExperts", requiredSet.size());
		progress.put("completedExperts", scoredSet.size());
		progress.put("completionPercentage",
				requiredSet.isEmpty() ? 0.0 : (scoredSet.size() * 100.0 / requiredSet.size()));
		// 可选：返回已评分专家列表
		List<String> scoredExperts = scoredPairs.stream().map(row -> (String) row.get("user_id")).distinct()
				.collect(Collectors.toList());
		progress.put("scoredExperts", scoredExperts);

		// 获取已评分专家账号
		List<String> scoredExpertsIds = jdbcTemplate.queryForList(
				"SELECT DISTINCT user_id FROM scores WHERE project_id = ? AND task_id = ? AND is_draft = 0",
				String.class, projectId, taskId);

		// 查询专家真实姓名
		List<String> scoredExpertNames = new ArrayList<>();
		if (!scoredExpertsIds.isEmpty()) {
			List<User> users = userRepository.findByUsernames(scoredExpertsIds);
			Map<String, String> usernameToName = users.stream()
					.collect(Collectors.toMap(User::getUsername, User::getName));
			for (String username : scoredExpertsIds) {
				scoredExpertNames.add(usernameToName.getOrDefault(username, username));
			}
		}

		progress.put("scoredExpertNames", scoredExpertNames);

		// === 关键补充：返回 isReviewed 字段 ===
    Integer isReviewed = null;
    try {
        isReviewed = jdbcTemplate.queryForObject(
            "SELECT is_reviewed FROM task_projects WHERE task_id = ? AND project_id = ?",
            Integer.class, taskId, projectId
        );
    } catch (Exception e) {
        isReviewed = 0; // 没查到时默认0
    }
    progress.put("isReviewed", isReviewed);

		return progress;
	}

	/**
	 * 获取项目评分详情
	 */
	@Override
	public Map<String, Object> getProjectScores(Long projectId, Long taskId) {
		Map<String, Object> result = new HashMap<>();

		// 获取项目基本信息
		Project project = projectRepository.selectById(projectId);
		if (project == null) {
			result.put("error", "项目不存在");
			return result;
		}

		result.put("project", convertToDTO(project));

		// 获取评分统计信息
		Map<String, Object> scoreStatistics;
		if (taskId != null) {
			scoreStatistics = getProjectScoreStatistics(projectId, taskId);
		} else {
			scoreStatistics = getProjectScoreStatistics(projectId);
		}
		result.put("statistics", scoreStatistics);

		// 获取项目评分进度
		Map<String, Object> progress;
		if (taskId != null) {
			progress = getProjectProgress(projectId, taskId);
		} else {
			progress = getProjectProgress(projectId);
		}
		result.put("progress", progress);

		return result;
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
		dto.setIndustry(project.getIndustry());

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

	/**
	 * 只查主表，不查关联的简化项目列表
	 */
	@Override
	public List<ProjectDTO> getAllSimpleProjects() {
		List<Project> projects = projectRepository.findAllByOrderByDisplayOrderAsc();
		return projects.stream().map(project -> {
			ProjectDTO dto = new ProjectDTO();
			dto.setId(project.getId());
			dto.setName(project.getName());
			dto.setStatus(project.getStatus());
			dto.setUnit(project.getUnit());
			dto.setLeader(project.getLeader());
			dto.setIndustry(project.getIndustry());
			dto.setCreateTime(project.getCreateTime());
			return dto;
		}).collect(Collectors.toList());
	}

	@Override
	public List<ProjectDTO> getSimpleProjectsByTask(Long taskId) {
		List<Project> projects = projectRepository.findByTaskIdOrderByDisplayOrderAsc(taskId);
		return projects.stream().map(project -> {
			ProjectDTO dto = new ProjectDTO();
			dto.setId(project.getId());
			dto.setName(project.getName());
			dto.setStatus(project.getStatus());
			dto.setUnit(project.getUnit());
			dto.setLeader(project.getLeader());
			dto.setIndustry(project.getIndustry());
			dto.setCreateTime(project.getCreateTime());
			dto.setIsReviewed(project.getIsReviewed());
			return dto;
		}).collect(Collectors.toList());
	}

	@Override
	public boolean isProjectReviewed(Long projectId, Long taskId) {
		// 1. 获取应评分总数
		int requiredCount = 0;
		int scoredCount = 0;
		try {
			requiredCount = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM task_experts_details WHERE task_id = ? AND project_id = ?", Integer.class,
					taskId, projectId);
			scoredCount = jdbcTemplate.queryForObject(
					"SELECT COUNT(DISTINCT sd.score_item_id + ':' + s.user_id) FROM score_details sd "
							+ "JOIN scores s ON sd.score_id = s.id "
							+ "WHERE s.project_id = ? AND s.task_id = ? AND s.is_draft = 0",
					Integer.class, projectId, taskId);
		} catch (Exception e) {
			return false;
		}
		return requiredCount > 0 && scoredCount == requiredCount;
	}

	private Map<String, Object> getProjectScoreStatistics(Long projectId) {
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
	private Map<String, Object> getProjectScoreStatistics(Long projectId, Long taskId) {
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

	private Double calculateProjectTotalScore(Long projectId) {
		List<Score> finalScores = scoreRepository.findFinalScoresByProjectId(projectId);

		if (finalScores.isEmpty()) {
			return 0.0;
		}

		// 计算总分
		double sum = finalScores.stream().mapToDouble(Score::getTotalScore).sum();

		return sum;
	}

	private Double calculateScoreItemAverage(Long projectId, Long scoreItemId) {
		return scoreRepository.calculateAverageScoreByProjectIdAndScoreItemId(projectId, scoreItemId);
	}

	@Override
	public List<Map<String, Object>> getProjectScoreDetails(Long projectId, Long taskId) {
		// 1. 获取所有评分项
		List<ScoreItem> scoreItems = scoreItemRepository.findByProjectId(projectId);
		// 2. 获取所有专家
		List<String> experts = jdbcTemplate.queryForList(
				"SELECT expert_username FROM task_experts WHERE task_id = ?", String.class, taskId
		);
		// 2.1 批量查找专家真实姓名
		Map<String, String> usernameToRealName = new HashMap<>();
		if (!experts.isEmpty()) {
			List<User> users = userRepository.findByUsernames(experts);
			for (User user : users) {
				usernameToRealName.put(user.getUsername(), user.getName());
			}
		}
		// 3. 获取所有评分
		List<Score> scores = scoreRepository.findByProjectIdAndTaskId(projectId, taskId);

		// === 补全每个Score的scores字段 ===
		for (Score score : scores) {
			List<Map<String, Object>> details = jdbcTemplate.queryForList(
				"SELECT score_item_id, score_value FROM score_details WHERE score_id = ?", score.getId());
			Map<Long, Integer> scoreMap = new HashMap<>();
			for (Map<String, Object> detail : details) {
				Long scoreItemId = ((Number) detail.get("score_item_id")).longValue();
				Integer scoreValue = ((Number) detail.get("score_value")).intValue();
				scoreMap.put(scoreItemId, scoreValue);
			}
			score.setScores(scoreMap);
		}

		// 4. 组装明细
		List<Map<String, Object>> result = new ArrayList<>();
		for (ScoreItem item : scoreItems) {
			for (String expert : experts) {
				// 查找该专家该评分项的分数
				Integer scoreValue = null;
				for (Score score : scores) {
					if (expert.equals(score.getUserId()) && score.getScores() != null && score.getScores().containsKey(item.getId())) {
						scoreValue = score.getScores().get(item.getId());
						break;
					}
				}
				Map<String, Object> row = new HashMap<>();
				row.put("scoreItemName", item.getName());
				// 优先用真实姓名
				row.put("expertName", usernameToRealName.getOrDefault(expert, expert));
				row.put("score", scoreValue);
				result.add(row);
			}
		}
		return result;
	}
}