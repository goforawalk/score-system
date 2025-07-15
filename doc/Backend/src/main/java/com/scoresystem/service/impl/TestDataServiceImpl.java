package com.scoresystem.service.impl;

import com.scoresystem.model.Project;
import com.scoresystem.model.ScoreItem;
import com.scoresystem.model.Task;
import com.scoresystem.model.User;
import com.scoresystem.repository.ProjectRepository;
import com.scoresystem.repository.ScoreItemRepository;
import com.scoresystem.repository.ScoreRepository;
import com.scoresystem.repository.TaskRepository;
import com.scoresystem.repository.UserRepository;
import com.scoresystem.repository.ScoreItemRoleRepository;
import com.scoresystem.service.TestDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class TestDataServiceImpl implements TestDataService {
	@Autowired
	private ProjectRepository projectRepository;
	@Autowired
	private TaskRepository taskRepository;
	@Autowired
	private ScoreItemRepository scoreItemRepository;
	@Autowired
	private ScoreRepository scoreRepository;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private ScoreItemRoleRepository scoreItemRoleRepository;

	@Override
	@Transactional
	public void clearAllTestData() {
		// 按照外键依赖关系删除数据，先删除引用表，再删除被引用表
		scoreRepository.deleteAllScores();
		scoreItemRoleRepository.deleteAllScoreItemRoles(); // 先删除评分项-角色关联
		scoreItemRepository.deleteAllScoreItems(); // 再删除评分项
		projectRepository.deleteAllProjects();
		taskRepository.deleteAllTaskExperts(); // 先删除任务-专家关联
		taskRepository.deleteAllTasks(); // 再删除任务
		// 可选：清空用户表（如只清理测试用户）
		// userRepository.deleteAll();
	}

	@Override
	@Transactional
	public String generateTestData() {
		clearAllTestData();
		int taskCount = 2;
		int minProject = 5, maxProject = 10;
		int scoreItemCount = 3;
		List<String> allExperts = Arrays.asList("expert1", "expert2", "expert3", "expert4", "expert5", "expert6",
				"expert7");
		List<List<String>> taskExperts = new ArrayList<>();
		List<String> experts1 = new ArrayList<>(allExperts.subList(0, 3));
		List<String> experts2 = new ArrayList<>(allExperts.subList(3, 6));
		taskExperts.add(experts1);
		taskExperts.add(experts2);
		List<Task> tasks = new ArrayList<>();
		List<Project> projects = new ArrayList<>();
		List<ScoreItem> scoreItems = new ArrayList<>();
		// 用于记录分配信息的Map
		Map<String, List<String>> taskExpertMap = new HashMap<>();
		Map<String, List<String>> projectScoreItemMap = new HashMap<>();
		Map<String, String> scoreItemRoleMap = new HashMap<>();
		
		Random rand = new Random();
		for (int t = 0; t < taskCount; t++) {
			Task task = new Task();
			task.setTaskType(t == 0 ? 1 : 2); // 示例：1=同步，2=异步
			
			if(task.getTaskType().equals(1)) {
				task.setCategory("同步 测试任务" + (t + 1));
			}else {
				task.setCategory("异步 测试任务" + (t + 1));
			}
			
			task.setStatus("active"); // 设置任务状态为活跃
			task.setScoreGroupType(1); // 设置评分组类型
			task.setStartTime(new Date()); // 设置开始时间
			task.setEndTime(new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L)); // 设置结束时间为7天后
			taskRepository.insert(task);
			tasks.add(task);
			
			// 记录任务-专家分配
			taskExpertMap.put(task.getCategory(), new ArrayList<>(taskExperts.get(t)));
			
			// 任务-专家分配
			for (String expert : taskExperts.get(t)) {
				taskRepository.insertTaskExpert(task.getId(), expert);
			}
			int projectNum = minProject + rand.nextInt(maxProject - minProject + 1);
			for (int p = 0; p < projectNum; p++) {
				Project project = new Project();
				project.setName("任务" + (t + 1) + "-项目" + (p + 1));
				project.setStatus("active"); // 设置项目状态为活跃
				project.setDescription("测试项目描述");
				project.setDisplayOrder(p + 1); // 设置显示顺序
				project.setUnit("测试单位");
				project.setLeader("测试负责人");
				project.setCreateTime(new Date());
				project.setUpdateTime(new Date());
				projectRepository.insert(project);
				projects.add(project);
				// 新增：插入任务-项目中间表
				taskRepository.insertTaskProject(task.getId(), project.getId());
				
				// 记录项目的评分项
				List<String> projectScoreItems = new ArrayList<>();
				
				for (int s = 0; s < scoreItemCount; s++) {
					ScoreItem item = new ScoreItem();
					item.setProjectId(project.getId());
					item.setName(project.getName() + "评分项" + (s + 1));
					item.setDescription(project.getName() + "测试描述");
					item.setWeight(1.0);
					item.setGroupType("preliminary");
					// 随机分数区间
					int minScore = 1 + rand.nextInt(5); // 1~5
					int maxScore = minScore + rand.nextInt(5) + 1; // min+1 ~ min+6
					item.setMinScore(minScore);
					item.setMaxScore(maxScore);
					// 评分项角色从本任务专家中随机选
					String role = taskExperts.get(t).get(rand.nextInt(3));
					item.setRole(role);
					scoreItemRepository.insert(item);
					scoreItems.add(item);
					
					// 记录评分项-角色分配
					projectScoreItems.add(item.getName());
					scoreItemRoleMap.put(item.getName(), role);
					
					// 插入评分项-角色关联
					scoreItemRoleRepository.saveScoreItemRole(item.getId(), role);
				}
				
				projectScoreItemMap.put(project.getName(), projectScoreItems);
			}
		}
		
		// 构建详细的返回信息
		StringBuilder result = new StringBuilder();
		result.append("=== 测试数据生成完成 ===\n");
		result.append("总计：任务").append(tasks.size()).append("个，项目").append(projects.size()).append("个，评分项").append(scoreItems.size()).append("个\n\n");
		
		// 详细分配信息
		result.append("=== 任务-专家分配详情 ===\n");
		for (Map.Entry<String, List<String>> entry : taskExpertMap.entrySet()) {
			result.append("任务：").append(entry.getKey()).append("\n");
			result.append("  专家：").append(String.join(", ", entry.getValue())).append("\n\n");
		}
		
		result.append("=== 项目-评分项-角色分配详情 ===\n");
		for (Map.Entry<String, List<String>> entry : projectScoreItemMap.entrySet()) {
			result.append("项目：").append(entry.getKey()).append("\n");
			for (String scoreItem : entry.getValue()) {
				String role = scoreItemRoleMap.get(scoreItem);
				result.append("  - ").append(scoreItem).append(" (负责专家: ").append(role).append(")\n");
			}
			result.append("\n");
		}
		
		return result.toString();
	}
}