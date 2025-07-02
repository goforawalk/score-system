package com.scoresystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scoresystem.dto.ScoreSystemModels.ProjectDTO;
import com.scoresystem.dto.ScoreSystemModels.TaskDTO;
import com.scoresystem.model.Project;
import com.scoresystem.model.Task;
import com.scoresystem.repository.ProjectRepository;
import com.scoresystem.repository.TaskRepository;
import com.scoresystem.service.ProjectService;
import com.scoresystem.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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
    private ProjectService projectService;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
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
        return tasks.stream()
                .map(task -> {
                    // 查询关联的专家和项目
                    loadTaskRelations(task);
                    return convertToDTO(task);
                })
                .collect(Collectors.toList());
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
        
        // 保存任务
        if (isNew) {
            taskRepository.insert(task);
        } else {
            taskRepository.updateById(task);
        }
        
        // 处理专家关系（先删除后插入）
        saveTaskExperts(task.getId(), taskDTO.getExperts());
        
        // 处理项目关系（先删除后插入）
        saveTaskProjects(task.getId(), taskDTO.getProjects());
        
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
        return tasks.stream()
                .map(task -> {
                    // 查询关联的专家和项目
                    loadTaskRelations(task);
                    return convertToDTO(task);
                })
                .collect(Collectors.toList());
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
        return jdbcTemplate.queryForList(
                "SELECT expert_username FROM task_experts WHERE task_id = ?",
                String.class,
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
                batchArgs.add(new Object[]{taskId, expert});
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO task_experts (task_id, expert_username) VALUES (?, ?)",
                    batchArgs);
        }
    }
    
    /**
     * 保存任务关联的项目
     */
    private void saveTaskProjects(Long taskId, List<ProjectDTO> projectDTOs) {
        // 删除旧关联
        jdbcTemplate.update("DELETE FROM task_projects WHERE task_id = ?", taskId);
        
        // 添加新关联
        if (projectDTOs != null && !projectDTOs.isEmpty()) {
            List<Object[]> batchArgs = new ArrayList<>();
            for (ProjectDTO projectDTO : projectDTOs) {
                batchArgs.add(new Object[]{taskId, projectDTO.getId()});
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO task_projects (task_id, project_id) VALUES (?, ?)",
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
        
        // 处理项目
        if (task.getProjects() != null) {
            dto.setProjects(task.getProjects().stream()
                    .filter(project -> project != null) // 过滤掉null项目
                    .map(project -> {
                        ProjectDTO projectDTO = projectService.getProjectById(project.getId());
                        return projectDTO != null ? projectDTO : new ProjectDTO(); // 防止空指针异常
                    })
                    .collect(Collectors.toList()));
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
    public TaskDTO completeTask(Long taskId, String username) {
        Task task = taskRepository.selectById(taskId);
        if (task == null) {
            return null;
        }
        
        // 设置任务为完成状态
        task.setStatus("completed");
        task.setEndTime(new Date());
        taskRepository.updateById(task);
        
        // 记录完成人
        jdbcTemplate.update(
                "UPDATE task_experts SET completed = 1, completion_time = ? WHERE task_id = ? AND expert_username = ?",
                new Date(), taskId, username);
        
        // 查询关联的专家和项目
        loadTaskRelations(task);
        
        return convertToDTO(task);
    }
}
