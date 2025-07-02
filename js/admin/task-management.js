class TaskManager {
  constructor() {
    this.projects = [];
  }

  // 添加项目
  addProject(project) {
    this.projects.push(project);
    this.renderProjectList(this.projects);
  }

  // 渲染项目列表
  renderProjectList(projects) {
    // 假设有一个方法可以渲染项目列表
    console.log("渲染项目列表:", projects);
  }

  // 添加自动排序功能
  sortProjects(projectIds) {
    return this.projects.sort((a, b) => {
      const indexA = projectIds.indexOf(a.id);
      const indexB = projectIds.indexOf(b.id);
      return indexA - indexB;
    });
  }

  // 更新项目显示顺序
  updateDisplayOrder(projectIds) {
    const sortedProjects = this.sortProjects(projectIds);
    this.renderProjectList(sortedProjects);
  }
}

// 示例用法
const taskManager = new TaskManager();
taskManager.addProject({ id: 1, name: "项目1" });
taskManager.addProject({ id: 2, name: "项目2" });
taskManager.addProject({ id: 3, name: "项目3" });

// 更新显示顺序
taskManager.updateDisplayOrder([3, 1, 2]);