$(document).ready(function() {
    // 验证用户权限 - 修复：检查角色是否以'expert'开头，支持expert1、expert2等
    if (!auth.isLoggedIn() || !auth.getRole().startsWith('expert')) {
        window.location.href = '../index.html';
        return;
    }

    // 检查URL参数，看是否是从"否"按钮返回
    const urlParams = new URLSearchParams(window.location.search);
    const returnToLast = urlParams.get('return_to_last');
    const forceClearCache = urlParams.get('force_clear_cache');

    // 如果URL参数指定强制清空缓存，则清空所有相关缓存
    if (forceClearCache === 'true') {
        console.log('强制清空缓存...');
        localStorage.removeItem('scores');
        localStorage.removeItem('latestReviewTaskId');
        
        // 清除所有评分草稿
        const username = auth.getUserInfo().username;
        const keysToRemove = [];
        for (let i = 0; i < localStorage.length; i++) {
            const key = localStorage.key(i);
            if (key && (key.startsWith(`scoreDraft_${username}_`) || key.startsWith('scoreDraft_'))) {
                keysToRemove.push(key);
            }
        }
        keysToRemove.forEach(key => localStorage.removeItem(key));
        
        // 移除URL参数
        window.history.replaceState({}, document.title, 'scoring.html');
        console.log('缓存已强制清空');
    }

    // 清除可能存在的旧数据以避免干扰
    // localStorage.removeItem('scores'); // 注释这行代码，使用API数据与localStorage合并的方式替代直接清除

    // 初始化页面
    initializeUserInfo();
    
    if (returnToLast === 'true') {
        // 如果是从"否"按钮返回，需要特殊处理显示最后一个项目
        // 移除URL参数以避免刷新页面时重复处理
        window.history.replaceState({}, document.title, 'scoring.html');
        loadTaskWithLastProject();
    } else {
        // 正常加载任务
    loadCurrentTask();
    }
    
    initAutoSave(); // 初始化自动保存

    // 绑定评分表单提交事件
    $('#scoreForm').on('submit', handleScoreSubmit);
    
    // 新增：绑定任务完成按钮事件
    $('#completeTaskBtn').on('click', showTaskCompletionDialog);
    
    // 新增：绑定任务完成确认对话框事件
    $('#confirmCompleteBtn').on('click', completeReviewTask);
    $('#cancelCompleteBtn').on('click', cancelTaskCompletion);
});

function initializeUserInfo() {
    const userInfo = auth.getUserInfo();
    $('#currentUser').text(userInfo.name);
    $('#logoutBtn').on('click', function(e) {
        e.preventDefault();
        // 添加确认框询问是否清空缓存
        if (confirm('是否清空本地缓存数据？\n选择"确定"将清空所有缓存，避免影响下一次测试。\n选择"取消"将保留缓存数据。')) {
            // 清空所有相关缓存数据
            clearAllScoreCache();
            console.log('已彻底清空所有评分相关缓存数据');
        } else {
            console.log('保留缓存数据');
        }
        // 清除登录信息并跳转到登录页
        auth.removeUserInfo();
        window.location.href = '../index.html';
    });
}

// 新增：彻底清理所有评分相关缓存
function clearAllScoreCache() {
    const username = auth.getUserInfo().username;
    const keysToRemove = [];
    for (let i = 0; i < localStorage.length; i++) {
        const key = localStorage.key(i);
        if (
            key &&
            (
                key === 'scores' ||
                key === 'latestReviewTaskId' ||
                key.startsWith(`scoreDraft_${username}_`) ||
                key.startsWith('scoreDraft_') ||
                key.startsWith('score') ||
                key.startsWith('draft') ||
                key.startsWith('review')
            )
        ) {
            keysToRemove.push(key);
        }
    }
    keysToRemove.forEach(key => localStorage.removeItem(key));
}

// 新增：加载当前任务
function loadCurrentTask() {
    showLoading();
    console.log("loadCurrentTask called");
    const username = auth.getUserInfo().username;
    
    // 先获取最新的评分数据，确保状态同步
    const taskId = localStorage.getItem('latestReviewTaskId');
    if (taskId) {
        // 如果有任务ID，先获取该任务的评分数据
        api.getScoresByUser(username, taskId)
            .then(scoresResponse => {
                if (scoresResponse.success && Array.isArray(scoresResponse.data)) {
                    // 更新本地存储的评分记录
                    localStorage.setItem('scores', JSON.stringify(scoresResponse.data));
                    console.log('已同步评分数据:', scoresResponse.data);
                }
                // 继续加载任务
                return loadTaskAndProjects(username);
            })
            .catch(error => {
                console.error('同步评分数据失败:', error);
                // 即使同步失败，也继续加载任务
                return loadTaskAndProjects(username);
            })
            .finally(() => {
                hideLoading();
            });
    } else {
        // 没有任务ID，直接加载任务
        loadTaskAndProjects(username).finally(() => {
            hideLoading();
        });
    }
}

// 新增：加载任务和项目的具体逻辑
function loadTaskAndProjects(username) {
    showLoading();
    return api.getActiveTaskWithProjects(username)
        .then(response => {
            if (response.success && response.data) {
                const { task, projectsInOrder } = response.data;
                if (!task) {
                    // 没有分配任务
                    $('.main-content').html(`
                        <div class="project-info">
                            <h2>暂无需要评分的评审任务</h2>
                            <p>请联系管理员分配评审任务。</p>
                        </div>
                    `);
                    return;
                }
                // 有任务但无可评分项目，直接跳转到评审总结页
                if (Array.isArray(projectsInOrder) && projectsInOrder.length === 0) {
                    // 记录当前任务ID，供 review-complete.html 使用
                    localStorage.setItem('latestReviewTaskId', task.id);
                    window.location.href = 'review-complete.html';
                    return;
                }
                // 有任务且有项目，显示任务名称
                $('#currentTaskName').remove();
                $('.main-content').prepend(`<div id="currentTaskName" style="font-size:18px;font-weight:bold;margin-bottom:16px;">当前评审任务：${task.category || task.id}</div>`);
                // 存储任务信息
                $('#scoreForm').data('taskId', task.id);
                $('#scoreForm').data('taskType', task.taskType);
                $('#scoreForm').data('scoreGroupType', task.scoreGroupType); // 保存评分组类型
                $('#scoreForm').data('projects', projectsInOrder);
                
                // 输出任务信息，便于调试
                console.log('当前任务信息:', {
                    id: task.id,
                    taskType: task.taskType,
                    scoreGroupType: task.scoreGroupType,
                    projectCount: projectsInOrder.length
                });
                
                if (task.taskType === 2) {
                    renderProjectNavigation(projectsInOrder);
                    loadCurrentProjectForTask2(projectsInOrder);
                } else {
                    loadCurrentProject();
                }
            } else {
                // 兜底：无任务
                $('.main-content').html(`
                    <div class="project-info">
                        <h2>暂无需要评分的评审任务</h2>
                        <p>请联系管理员分配评审任务。</p>
                    </div>
                `);
            }
        })
        .catch(error => {
            console.error('加载任务失败:', error);
            showError('加载任务失败，请刷新页面重试');
        })
        .finally(() => {
            hideLoading();
        });
}

// 新增：任务类型2的项目加载
function loadCurrentProjectForTask2(projects) {
    showLoading();
    const username = auth.getUserInfo().username;
    const userRole = auth.getRole();
    
    console.log('=== loadCurrentProjectForTask2 开始 ===');
    console.log('传入的项目数组:', projects);
    console.log('用户角色:', userRole);
    console.log('用户名:', username);
    
    if (!Array.isArray(projects) || projects.length === 0) {
        console.log('项目数组为空或无效');
        showNoProjectMessage();
        return;
    }
    
    // 检查原始项目数据的完整性
    projects.forEach((project, index) => {
        console.log(`项目${index} (ID: ${project.id}) 原始数据:`, {
            name: project.name,
            hasScoreItems: Array.isArray(project.scoreItems),
            scoreItemsCount: Array.isArray(project.scoreItems) ? project.scoreItems.length : 0,
            hasScoreGroups: !!project.scoreGroups,
            scoreGroupsKeys: project.scoreGroups ? Object.keys(project.scoreGroups) : [],
            scoreItemsWithRoles: Array.isArray(project.scoreItems) ? project.scoreItems.filter(item => item.roles && item.roles.includes(userRole)).length : 0
        });
    });
    
    // 获取评分记录
    api.getProjectScores(projects[0].id, $('#scoreForm').data('taskId'))
        .then(scoresResponse => {
            if (scoresResponse.success) {
                // 根据API返回的数据结构，提取评分记录
                let apiScores = [];
                if (scoresResponse.data) {
                    // 如果是mock API，评分记录在 response.data.details 中
                    if (scoresResponse.data.details && Array.isArray(scoresResponse.data.details)) {
                        apiScores = scoresResponse.data.details;
                    }
                    // 如果是后端API，评分记录在 response.data 中（需要进一步处理）
                    else if (scoresResponse.data.statistics) {
                        // 后端API返回的是项目详情，不包含评分记录
                        apiScores = [];
                    }
                }
                console.log('API评分记录:', apiScores);
                
                // 合并API评分和本地存储的评分记录
                const localScores = JSON.parse(localStorage.getItem('scores') || '[]');
                console.log('本地评分记录:', localScores);
                
                const mergedScores = [...apiScores];
                
                // 将本地存储中的评分添加到合并列表中 (如果在API中不存在)
                localScores.forEach(localScore => {
                    const exists = mergedScores.some(s => 
                        s.projectId === localScore.projectId && s.username === localScore.username
                    );
                    if (!exists) {
                        mergedScores.push(localScore);
                    }
                });
                
                // 将合并后的评分记录保存回localStorage，确保同步
                localStorage.setItem('scores', JSON.stringify(mergedScores));
                console.log('合并后的评分记录:', mergedScores);
                
                // 找到第一个未完成的项目
                const currentProject = projects.find(project => {
                    const hasScored = mergedScores.some(s => 
                        s.projectId === project.id && s.username === username
                    );
                    return !hasScored;
                });
                
                console.log('找到的当前项目:', currentProject);
                
                if (currentProject) {
                    // 修复：创建项目副本，避免修改原始数据
                    const projectCopy = JSON.parse(JSON.stringify(currentProject)); // 深拷贝
                    
                    console.log('项目副本创建完成:', {
                        id: projectCopy.id,
                        name: projectCopy.name,
                        hasScoreItems: Array.isArray(projectCopy.scoreItems),
                        scoreItemsCount: Array.isArray(projectCopy.scoreItems) ? projectCopy.scoreItems.length : 0,
                        hasScoreGroups: !!projectCopy.scoreGroups,
                        scoreGroupsKeys: projectCopy.scoreGroups ? Object.keys(projectCopy.scoreGroups) : []
                    });
                    
                    // 过滤出专家可评分的项目
                    const filteredScoreItems = Array.isArray(projectCopy.scoreItems) ? projectCopy.scoreItems.filter(item =>
                        item.roles && item.roles.includes(userRole)
                    ) : [];
                    projectCopy.scoreItems = filteredScoreItems;
                    
                    console.log('loadCurrentProjectForTask2 - 项目数据:', projectCopy);
                    console.log('loadCurrentProjectForTask2 - 过滤后的评分项:', filteredScoreItems);
                    console.log('过滤条件 - 用户角色:', userRole);
                    console.log('过滤条件 - 评分项角色:', Array.isArray(projectCopy.scoreItems) ? projectCopy.scoreItems.map(item => item.roles) : []);
                    
                    // 设置当前项目索引
                    const currentIndex = projects.findIndex(p => p.id === currentProject.id);
                    $('#scoreForm').data('currentProjectIndex', currentIndex);
                    
                    console.log('=== 准备调用 renderProject ===');
                    renderProject(projectCopy);
                    updateProjectNavigation(projects, currentIndex);
                } else {
                    // 所有项目都已完成
                    console.log('所有项目都已完成');
                    showAllProjectsCompleted();
                }
            }
        })
        .catch(error => {
            console.error('加载项目失败:', error);
            showError('加载项目失败，请刷新页面重试');
        })
        .finally(() => {
            hideLoading();
        });
}

// 新增：渲染项目导航
function renderProjectNavigation(projects) {
    const $navigation = $('#projectNavigation');
    const $icons = $('#projectIcons');
    const username = auth.getUserInfo().username;
    const taskId = $('#scoreForm').data('taskId');
    
    $icons.empty();
    
    // 先同步最新的评分数据，确保状态正确
    const syncScoresAndRender = () => {
        // 过滤出当前用户有权限评分的项目
        const userRole = auth.getRole();
        const userProjects = projects.filter(project => {
            // 检查项目是否有评分项且用户有权限
            if (Array.isArray(project.scoreItems)) {
                return project.scoreItems.some(item => 
                    item.roles && item.roles.includes(userRole)
                );
            }
            // 如果有scoreGroups，检查当前评分组类型
            if (project.scoreGroups) {
                const scoreGroupType = $('#scoreForm').data('scoreGroupType') || 'preliminary';
                const scoreItems = project.scoreGroups[scoreGroupType];
                if (Array.isArray(scoreItems)) {
                    return scoreItems.some(item => 
                        item.roles && item.roles.includes(userRole)
                    );
                }
            }
            return false;
        });
        
        // 只渲染用户有权限的项目
        userProjects.forEach((project, userIndex) => {
            const originalIndex = projects.findIndex(p => p.id === project.id);
        const isCompleted = checkProjectCompleted(project.id);
        const iconClass = isCompleted ? 'completed' : 'pending';
        
        $icons.append(`
                <div class="project-icon ${iconClass}" id="projectIcon_${project.id}" data-project-index="${originalIndex}" data-user-index="${userIndex}" data-project-id="${project.id}">
                    ${userIndex + 1}
            </div>
        `);
    });
    
    // 绑定点击事件
    $('.project-icon').off('click').on('click', function() {
        const projectIndex = $(this).data('project-index');
        switchToProject(projectIndex);
    });
    
    $navigation.show();
    };
    
    // 如果有任务ID，先同步评分数据
    if (taskId) {
        api.getScoresByUser(username, taskId)
            .then(scoresResponse => {
                if (scoresResponse.success && Array.isArray(scoresResponse.data)) {
                    // 更新本地存储的评分记录
                    localStorage.setItem('scores', JSON.stringify(scoresResponse.data));
                    console.log('渲染项目导航前已同步评分数据:', scoresResponse.data);
                }
                // 渲染项目导航
                syncScoresAndRender();
            })
            .catch(error => {
                console.error('同步评分数据失败:', error);
                // 即使同步失败，也渲染项目导航
                syncScoresAndRender();
            })
            .finally(() => {
                hideLoading();
            });
    } else {
        // 没有任务ID，直接渲染项目导航
        syncScoresAndRender();
    }
}

// 新增：更新项目导航状态
function updateProjectNavigation(projects, currentIndex) {
    $('.project-icon').each(function() {
        const index = $(this).data('project-index');
        const project = projects[index];
        const isCompleted = checkProjectCompleted(project.id);
        
        $(this).removeClass('current pending completed');
        if (index === currentIndex) {
            $(this).addClass('current');
        } else if (isCompleted) {
            $(this).addClass('completed');
        } else {
            $(this).addClass('pending');
        }
    });
}

// 新增：检查项目是否已完成
function checkProjectCompleted(projectId) {
    const username = auth.getUserInfo().username;
    // 1. 检查正式评分
    const localScores = JSON.parse(localStorage.getItem('scores') || '[]');
    const userScore = localScores.find(s => s.projectId === projectId && s.username === username);
    if (userScore && Array.isArray(userScore.scores)) {
        // 只要所有评分项都被填写（input有值），就算已完成
        const allFilled = userScore.scores.length > 0 && userScore.scores.every(item => item.score !== null && item.score !== undefined && item.score !== '');
        if (allFilled) return true;
    }
    // 2. 检查草稿
    const draftKey = `scoreDraft_${username}_${projectId}`;
    const draft = localStorage.getItem(draftKey);
    if (draft) {
        const draftScores = JSON.parse(draft);
        const allFilled = Array.isArray(draftScores) && draftScores.length > 0 && draftScores.every(item => item.score !== null && item.score !== undefined && item.score !== '');
        if (allFilled) return true;
    }
    return false;
}

// 新增：项目切换功能
function switchToProject(projectIndex) {
    console.log('=== switchToProject 开始 ===');
    console.log('切换到的项目索引:', projectIndex);
    
    showLoading();
    // 切换前自动保存当前项目草稿
    saveScoreDraft();
    
    // 新增：自动提交当前项目的评分
    const currentProjectId = $('#scoreForm').data('projectId');
    if (currentProjectId) {
        autoSubmitCurrentProjectScore(currentProjectId);
    }
    
    const projects = $('#scoreForm').data('projects');
    const username = auth.getUserInfo().username;
    const userRole = auth.getRole();
    
    console.log('switchToProject - 项目数组:', projects);
    console.log('switchToProject - 用户角色:', userRole);
    console.log('switchToProject - 用户名:', username);
    
    if (projectIndex >= 0 && projectIndex < projects.length) {
        // 修复：创建项目副本，避免修改原始数据
        const originalProject = projects[projectIndex];
        console.log('switchToProject - 原始项目数据:', originalProject);
        
        const project = JSON.parse(JSON.stringify(originalProject)); // 深拷贝
        console.log('switchToProject - 项目副本创建完成:', project);
        
        // 设置当前项目索引
        $('#scoreForm').data('currentProjectIndex', projectIndex);
        
        // 修正：兼容 scoreGroups 和 scoreItems
        const scoreGroupType = $('#scoreForm').data('scoreGroupType') || 'preliminary';
        let filteredScoreItems = [];
        
        console.log('switchToProject - 开始过滤评分项...');
        console.log('switchToProject - scoreGroupType:', scoreGroupType);
        console.log('switchToProject - project.scoreGroups:', project.scoreGroups);
        console.log('switchToProject - project.scoreItems:', project.scoreItems);
        
        if (project.scoreGroups && project.scoreGroups[scoreGroupType]) {
            console.log(`switchToProject - 从scoreGroups[${scoreGroupType}]获取评分项:`, project.scoreGroups[scoreGroupType]);
            filteredScoreItems = project.scoreGroups[scoreGroupType].filter(item =>
                item.roles && item.roles.includes(userRole)
            );
            console.log(`switchToProject - scoreGroups过滤后:`, filteredScoreItems);
        } else if (Array.isArray(project.scoreItems)) {
            console.log('switchToProject - 从scoreItems获取评分项:', project.scoreItems);
            filteredScoreItems = project.scoreItems.filter(item =>
                item.roles && item.roles.includes(userRole)
            );
            console.log(`switchToProject - scoreItems过滤后:`, filteredScoreItems);
        }
        
        project.scoreItems = filteredScoreItems;
        
        console.log('switchToProject - 项目数据:', project);
        console.log('switchToProject - 过滤后的评分项:', filteredScoreItems);
        console.log('switchToProject - 过滤条件 - 用户角色:', userRole);
        console.log('switchToProject - 过滤条件 - 评分项角色:', filteredScoreItems.map(item => item.roles));
        
        console.log('=== 准备调用 renderProject ===');
        renderProject(project);
        
        // 更新导航状态
        updateProjectNavigation(projects, projectIndex);
        // 检查是否所有项目都已完成
        checkAllProjectsCompleted();
        
        // 修复：重新绑定表单提交事件，确保切换项目后可以正常提交评分
        $('#scoreForm').off('submit').on('submit', handleScoreSubmit);
        
        // 修复：重新启用表单
        $('#scoringForm').removeClass('disabled');
        
        // 修复：重新初始化自动保存功能
        initAutoSave();
        
        console.log('已切换到项目索引:', projectIndex, '并重新绑定事件');
    } else {
        console.error('switchToProject - 项目索引无效:', projectIndex);
    }
    
    setTimeout(() => {
        hideLoading();
    }, 0);
    
    console.log('=== switchToProject 完成 ===');
}

// 新增：自动提交当前项目的评分（无需API调用，只更新本地存储）
function autoSubmitCurrentProjectScore(projectId) {
    const scores = [];
    const username = auth.getUserInfo().username;
    let hasUserInput = false;
    
    // 收集当前表单中的评分
    $('input[type="number"]').each(function() {
        const $input = $(this);
        const inputValue = $input.val();
        // 检查用户是否实际输入了值（不为空）
        if (inputValue !== '') {
            hasUserInput = true;
        const score = parseInt(inputValue);
        const scoreValue = isNaN(score) ? 0 : score;
        scores.push({
            itemId: parseInt($input.data('item-id')),
            score: scoreValue
        });
        }
    });
    
    // 如果没有评分项或没有输入，不自动保存
    if (scores.length === 0 || !hasUserInput) {
        const draftKey = `scoreDraft_${username}_${projectId}`;
        localStorage.removeItem(draftKey);
        console.log('没有评分项或未输入，不自动提交');
        return;
    }
    
    // 修复：只有用户实际输入了值，才保存到正式评分记录
        // 构建评分数据
        const taskId = $('#scoreForm').data('taskId');
        const scoreData = {
            projectId: projectId,
            taskId: taskId,
            username: username,
            scores: scores,
            totalScore: 0,
            comments: '',
            isDraft: false
        };
        
        // 更新本地存储
        const existingScores = JSON.parse(localStorage.getItem('scores') || '[]');
        // 移除该项目的旧评分记录
        const filteredScores = existingScores.filter(s => 
            !(s.projectId === projectId && s.username === username)
        );
        // 添加新评分记录
        filteredScores.push(scoreData);
        localStorage.setItem('scores', JSON.stringify(filteredScores));
        
        console.log(`自动保存用户 ${username} 的评分数据到本地存储:`, scoreData);
    
    // 总是更新草稿数据
    const draftKey = `scoreDraft_${username}_${projectId}`;
    localStorage.setItem(draftKey, JSON.stringify(scores));
}

// 新增：渲染只读项目 - 实际上我们现在允许修改评分
function renderProjectReadOnly(project) {
    const username = auth.getUserInfo().username;
    
    // 渲染项目信息
    const $infoContent = $('.info-content');
    $infoContent.html(`
        <div class="info-item">
            <label>项目名称：</label>
            <span>${project.name}</span>
        </div>
        <div class="info-item">
            <label>项目描述：</label>
            <span>${project.description || '暂无描述'}</span>
        </div>
        <div class="info-item">
            <label>状态：</label>
            <span style="color: #52c41a;">已完成评分</span>
        </div>
    `);

    // 获取已提交的评分
    const scores = JSON.parse(localStorage.getItem('scores') || '[]');
    const userScores = scores.find(s => 
        s.projectId === project.id && s.username === username
    );

    // 渲染评分项（可修改）
    const $scoreItems = $('.score-items');
    $scoreItems.empty();

    if (Array.isArray(project.scoreItems)) {
        project.scoreItems.forEach((item, index) => {
            // 修复：从用户评分记录中查找对应的评分项，使用item.id
            const scoreItem = userScores ? userScores.scores.find(s => s.itemId === item.id) : null;
            const score = scoreItem ? scoreItem.score : 0;
            
            $scoreItems.append(`
                <div class="score-item">
                    <h4>${item.name}<span class="score-range">分值区间：${item.minScore || 0}-${item.maxScore || 100}分</span></h4>
                    <div class="score-input">
                        <input type="number" 
                               name="scores[${item.id}]" 
                               min="${item.minScore || 0}" 
                               max="${item.maxScore || 100}" 
                               required 
                               data-item-id="${item.id}"
                               placeholder="请输入${item.minScore || 0}-${item.maxScore || 100}的分数"
                               value="${score}">
                        <span>分</span>
                    </div>
                </div>
            `);
        });
    }

    // 显示提交按钮
    $('.btn-submit').show();
    $('#scoringForm').removeClass('readonly');
    
    // 加载评分草稿（可以再次调用，确保表单字段都已填充）
    loadScoreDraft();
}

// 新增：检查所有项目是否已完成
function checkAllProjectsCompleted() {
    const projects = $('#scoreForm').data('projects');
    const username = auth.getUserInfo().username;
    const userRole = auth.getRole();
    // 过滤出用户有权限评分的项目
    const userProjects = projects.filter(project => {
        if (Array.isArray(project.scoreItems)) {
            return project.scoreItems.some(item => 
                item.roles && item.roles.includes(userRole)
            );
        }
        if (project.scoreGroups) {
            const scoreGroupType = $('#scoreForm').data('scoreGroupType') || 'preliminary';
            const scoreItems = project.scoreGroups[scoreGroupType];
            if (Array.isArray(scoreItems)) {
                return scoreItems.some(item => 
                    item.roles && item.roles.includes(userRole)
                );
            }
        }
        return false;
    });
    
    const allCompleted = Array.isArray(userProjects) ? userProjects.every(project => checkProjectCompleted(project.id)) : true;
    
    if (allCompleted) {
        $('#completeTaskBtn').show();
    } else {
        $('#completeTaskBtn').hide();
    }
}

// 新增：显示所有项目完成状态
function showAllProjectsCompleted() {
    // 保存当前任务ID到localStorage，确保后续能获取到任务ID
    const taskId = $('#scoreForm').data('taskId');
    if (taskId) {
        localStorage.setItem('latestReviewTaskId', taskId);
        console.log('保存任务ID到localStorage:', taskId);
    }
    
    // 获取所有项目
    const projects = $('#scoreForm').data('projects');
    
    // 在所有项目都已完成的判断前，先判断专家是否有可评分项
    const username = auth.getUserInfo().username;
    const userRole = auth.getRole();
    const hasScorableItems = Array.isArray(projects) && projects.some(project => 
        Array.isArray(project.scoreItems) && project.scoreItems.some(item => 
            item.roles && item.roles.includes(userRole)
        )
    );
    
    if (hasScorableItems) {
        $('.main-content').html(`
            <div class="project-info">
                <h2>所有项目评分已完成</h2>
                <p>您已完成所有项目的评分，可以结束本次评审任务。</p>
                <div class="form-actions">
                    <button type="button" class="btn btn-complete-task" onclick="showTaskCompletionDialog()">完成评审任务</button>
                </div>
            </div>
        `);
    } else {
        $('.main-content').html(`
            <div class="project-info">
                <h2>您未被分配任何评分项，请联系管理员</h2>
                <p>当前没有需要您评分的项目，请稍后再试。</p>
            </div>
        `);
    }
}

// 新增：显示任务完成确认对话框
function showTaskCompletionDialog() {
    console.log("显示任务完成确认对话框");
    $('#taskCompletionDialog').css('display', 'flex');
    
    // 确保绑定确认按钮事件
    $('#confirmCompleteBtn').off('click').on('click', function() {
        completeReviewTask();
    });
    
    // 确保绑定取消按钮事件
    $('#cancelCompleteBtn').off('click').on('click', function() {
        cancelTaskCompletion();
    });
}

// 新增：取消任务完成，返回最后一个项目
function cancelTaskCompletion() {
    console.log("执行取消任务完成操作，重新加载页面...");
    // 隐藏对话框
    $('#taskCompletionDialog').hide();
    // 使用更简单直接的方式：刷新页面并添加参数
    window.location.href = 'scoring.html?return_to_last=true';
    return;
}

// 新增：完成评审任务
function completeReviewTask() {
    disableSubmitBtn();
    showLoading();
    // 主要从表单数据获取taskId，如果不存在则尝试从localStorage获取
    const taskId = $('#scoreForm').data('taskId') || localStorage.getItem('latestReviewTaskId') || "task-002";
    const username = auth.getUserInfo().username;
    
    console.log("尝试完成任务，任务ID:", taskId);
    
    // 确保任务ID保存到localStorage
    localStorage.setItem('latestReviewTaskId', taskId);
    
    // 获取本地存储的评分记录
    const localScores = JSON.parse(localStorage.getItem('scores') || '[]');
    console.log("本地评分记录:", localScores);
    
    // 过滤出当前用户的评分记录
    const userScores = localScores.filter(s => s.username === username);
    console.log("当前用户的本地评分记录:", userScores);
    
    // 获取所有项目
    const projects = $('#scoreForm').data('projects');
    console.log("所有项目:", projects);
    
    // 确保每个项目都有评分记录
    let allSubmitPromises = [];
    
    if (Array.isArray(projects)) {
        projects.forEach(project => {
            // 检查本地是否有该项目的评分记录
            const hasLocalScore = userScores.some(s => s.projectId === project.id);
            
            if (!hasLocalScore) {
                console.log(`项目 ${project.id} 在本地没有评分记录，尝试从草稿中获取`);
                
                // 尝试从本地草稿中获取评分并提交
                const draftKey = `scoreDraft_${username}_${project.id}`;
                const draft = localStorage.getItem(draftKey);
                
                if (draft) {
                    const draftScores = JSON.parse(draft);
                    if (Array.isArray(draftScores) && draftScores.length > 0) {
                        // 检查是否有有效评分（非0分）
                        const hasValidScore = draftScores.some(item => item.score > 0);
                        
                        if (hasValidScore) {
                            console.log(`为项目 ${project.id} 从草稿中创建评分数据`);
                            const scoreData = {
                                projectId: project.id,
                                taskId: taskId,
                                username: username,
                                scores: draftScores,
                                totalScore: 0,
                                comments: '',
                                isDraft: false
                            };
                            
                            // 添加到提交队列
                            allSubmitPromises.push(
                                api.submitScore(scoreData)
                                    .then(response => {
                                        console.log(`为项目 ${project.id} 从草稿提交评分成功:`, response);
                                        return response;
                                    })
                                    .catch(error => {
                                        console.error(`为项目 ${project.id} 从草稿提交评分失败:`, error);
                                        throw error;
                                    })
                            );
                        }
                    }
                }
            } else {
                // 确保已有的评分记录也被提交到API
                const existingScoreData = userScores.find(s => s.projectId === project.id);
                if (existingScoreData) {
                    console.log(`确保项目 ${project.id} 的评分记录已提交到API`);
                    // 确保数据格式完整
                    const scoreData = {
                        projectId: existingScoreData.projectId,
                        taskId: taskId,
                        username: existingScoreData.username,
                        scores: existingScoreData.scores,
                        totalScore: existingScoreData.totalScore || 0,
                        comments: existingScoreData.comments || '',
                        isDraft: existingScoreData.isDraft || false
                    };
                    
                    allSubmitPromises.push(
                        api.submitScore(scoreData)
                            .then(response => {
                                console.log(`重新提交项目 ${project.id} 的评分成功:`, response);
                                return response;
                            })
                            .catch(error => {
                                console.error(`重新提交项目 ${project.id} 的评分失败:`, error);
                                throw error;
                            })
                    );
                }
            }
        });
    }
    
    // 等待所有评分提交完成
    Promise.all(allSubmitPromises)
        .then(responses => {
            console.log("所有评分记录已提交到API，响应:", responses);
            
            // 获取最新的评分记录
            return api.getScoresByUser(username, taskId);
        })
        .then(scoresResponse => {
            if (scoresResponse.success && Array.isArray(scoresResponse.data)) {
                // 过滤出当前用户的评分记录
                const apiUserScores = scoresResponse.data.filter(s => s.username === username);
                console.log("API中当前用户的评分记录:", apiUserScores);
                
                // 更新本地存储
                localStorage.setItem('scores', JSON.stringify(apiUserScores));
                console.log('更新本地评分记录:', apiUserScores);
            }
            
            // 调用API完成任务
            return api.completeReviewTask(taskId, username);
        })
        .then(response => {
            if (response.success) {
                console.log("任务完成API调用成功，响应:", response);
                
                // 检查任务是否真的完成了（所有专家都完成）
                return api.checkTaskCompletionStatus(taskId);
            } else {
                throw new Error(response.message || '未知错误');
            }
        })
        .then(completionStatusResponse => {
            if (completionStatusResponse.success) {
                const status = completionStatusResponse.data;
                console.log("任务完成状态:", status);
                
                if (status.isCompleted) {
                    // 任务真的完成了，跳转到评审总结页面
                    console.log("所有专家都已完成评审，任务完成");
                    window.location.href = 'review-complete.html';
                } else {
                    // 任务还没有完成，显示等待其他专家的消息
                    console.log("当前专家已完成，但还有其他专家未完成");
                    $('#taskCompletionDialog').hide();
                    
                    $('.main-content').html(`
                        <div class="project-info">
                            <h2>您已完成评审工作</h2>
                            <p>您已完成所有项目的评分，正在等待其他专家完成评审。</p>
                            <div class="completion-status">
                                <p>完成进度：${status.completedExperts}/${status.totalExperts} 位专家已完成</p>
                                <p>完成百分比：${status.completionPercentage.toFixed(1)}%</p>
                                ${status.completedExpertsList.length > 0 ? 
                                    `<p>已完成的专家：${status.completedExpertsList.join(', ')}</p>` : ''}
                            </div>
                            <div class="form-actions">
                                <button type="button" class="btn btn-primary" onclick="refreshTaskStatus()">刷新页面</button>
                            </div>
                        </div>
                    `);
                }
            } else {
                throw new Error(completionStatusResponse.message || '检查任务完成状态失败');
            }
        })
        .catch(error => {
            console.error("完成任务失败:", error);
            alert('完成任务失败：' + (error.message || '未知错误'));
        })
        .finally(() => {
            enableSubmitBtn();
            hideLoading();
    });
}

function loadCurrentProject() {
    const projects = $('#scoreForm').data('projects');
    const taskType = $('#scoreForm').data('taskType');
    const username = auth.getUserInfo().username;
    
    if (!projects || !Array.isArray(projects) || projects.length === 0) {
        console.log('没有可用的项目');
        showNoProjectMessage();
        return;
    }
    
    // 清除可能存在的轮询
    if (window.progressPollingInterval) {
        clearInterval(window.progressPollingInterval);
        window.progressPollingInterval = null;
    }
    
    // 隐藏等待消息
    $('#waitingMessage').hide();
    
    // 获取最新的评分记录
    api.getProjectScores(projects[0].id, $('#scoreForm').data('taskId'))
                    .then(response => {
            // 根据API返回的数据结构，提取评分记录
            let apiScores = [];
            if (response.success && response.data) {
                // 如果是mock API，评分记录在 response.data.details 中
                if (response.data.details && Array.isArray(response.data.details)) {
                    apiScores = response.data.details;
                }
                // 如果是后端API，评分记录在 response.data 中（需要进一步处理）
                else if (response.data.statistics) {
                    // 后端API返回的是项目详情，不包含评分记录
                    // 这里需要调用专门的评分记录API
                    apiScores = [];
                }
            }
            
            // 同步到本地存储
            localStorage.setItem('scores', JSON.stringify(apiScores));
            console.log("已更新本地评分记录:", apiScores);
            
            // 如果是同步评审模式(taskType=1)，要检查当前所有项目的完成状态
            if (taskType === 1) {
                // 获取所有项目的评分进度
                const checkProgress = async () => {
                    console.log("同步评审模式：检查所有项目进度");
                    
                    for (let i = 0; i < projects.length; i++) {
                        try {
                            const projectId = projects[i].id;
                            const taskId = $('#scoreForm').data('taskId');
                            const response = await api.getProjectScoringProgress(projectId, taskId);
                            
                        if (response.success) {
                                const progress = response.data;
                                console.log(`项目${projectId}进度:`, progress);
                                
                                // 检查当前用户是否已完成该项目评分
                                const userCompleted = progress.scoredExperts && progress.scoredExperts.includes(username);
                                
                                // 如果项目未完成评分，并且当前用户未完成评分，则渲染该项目
                                if (!progress.completed) {
                                    // 如果当前用户未完成评分，则显示该项目
                                    if (!userCompleted) {
                                        console.log(`用户${username}未完成项目${projectId}评分，加载该项目`);
                                        // 修复：创建项目副本
                                        const projectCopy = JSON.parse(JSON.stringify(projects[i]));
                                        renderProject(projectCopy);
                                        updateProjectNavigation(projects, i);
                                        $('#scoreForm').data('currentProjectIndex', i);
                                        return;
                                } else {
                                        // 用户已完成，但其他专家未完成，显示等待
                                        console.log(`用户${username}已完成项目${projectId}评分，但需等待其他专家`);
                                        showWaitingMessage(projectId);
                                        $('#currentProjectName').text(projects[i].name);
                                        updateScoringProgress(projectId);
                                        startProgressPolling(projectId);
                                        return;
                                    }
                                }
                            }
                        } catch (error) {
                            console.error(`获取项目${projects[i].id}进度失败:`, error);
                        }
                    }
                    
                    // 如果所有项目都完成了，显示完成任务界面
                    console.log("所有项目已完成评分");
                    showAllProjectsCompleted();
                };
                
                checkProgress();
                            } else {
                // 任务类型2：找到第一个未完成的项目
                let nextProjectIndex = -1;
                let allCompleted = true;
                
                for (let i = 0; i < projects.length; i++) {
                    const completed = checkProjectCompleted(projects[i].id);
                    if (!completed) {
                        allCompleted = false;
                        nextProjectIndex = i;
                        break;
                    }
                }
                
                if (allCompleted) {
                    console.log('所有项目已完成评分');
                    showAllProjectsCompleted();
                    return;
                }
                
                if (nextProjectIndex === -1) {
                    console.log('未找到未完成的项目，但allCompleted为false，这是一个异常情况');
                    showTaskCompletionDialog();
                    return;
                }
                
                console.log(`加载项目索引: ${nextProjectIndex}`);
                
                // 存储当前项目索引
                $('#scoreForm').data('currentProjectIndex', nextProjectIndex);
                
                // 修复：创建项目副本
                const originalProject = projects[nextProjectIndex];
                const project = JSON.parse(JSON.stringify(originalProject));
                console.log('加载项目:', project);
                
                // 确保UI显示正确
                $('#scoringForm').show();
                
                // 渲染项目
                renderProject(project);
                
                // 更新项目导航状态
                updateProjectNavigation(projects, nextProjectIndex);
                
                // 检查是否所有项目都已完成
                checkAllProjectsCompleted();
            }
        });
}

function renderProject(project) {
    // 确保评分表单可见
    $('.scoring-form').show();
    $('#scoringForm').show();

    const username = auth.getUserInfo().username;
    const userRole = auth.getRole();
    const taskType = $('#scoreForm').data('taskType');
    const scoreGroupType = $('#scoreForm').data('scoreGroupType') || 'preliminary';
    
    console.log('=== renderProject 开始 ===');
    console.log(`渲染项目: ${project.id}, 任务类型: ${taskType}, 评分组类型: ${scoreGroupType}`);
    console.log('项目详情:', project);
    console.log('用户角色:', userRole);
    console.log('用户名:', username);
    
    // 检查项目数据结构
    console.log('项目数据结构检查:', {
        hasScoreItems: Array.isArray(project.scoreItems),
        scoreItemsCount: Array.isArray(project.scoreItems) ? project.scoreItems.length : 0,
        hasScoreGroups: !!project.scoreGroups,
        scoreGroupsKeys: project.scoreGroups ? Object.keys(project.scoreGroups) : [],
        scoreGroupType: scoreGroupType,
        hasScoreGroupType: project.scoreGroups && project.scoreGroups[scoreGroupType] ? true : false,
        scoreGroupTypeItemsCount: project.scoreGroups && project.scoreGroups[scoreGroupType] ? project.scoreGroups[scoreGroupType].length : 0
    });
    
    // 渲染项目信息
    const $infoContent = $('.info-content');
    $infoContent.html(`
        <div class="info-item">
            <label>项目名称：</label>
            <span>${project.name}</span>
        </div>
        <div class="info-item">
            <label>项目描述：</label>
            <span>${project.description || '暂无描述'}</span>
        </div>
        <div class="info-item">
            <label>任务类型：</label>
            <span>${taskType === 1 ? '类型1 (同步评审)' : '类型2 (异步评审)'}</span>
        </div>
    `);

    // 获取已提交的评分记录
    const scores = JSON.parse(localStorage.getItem('scores') || '[]');
    console.log(`渲染项目 ${project.id} 的评分记录, 本地评分记录数:`, scores.length);
    console.log('所有评分记录:', scores);
    
    // 确保scores是数组
    const scoresArray = Array.isArray(scores) ? scores : [];
    
    const userScores = scoresArray.find(s => {
        const match = s.projectId === project.id && s.username === username;
        if (match) {
            console.log(`找到项目 ${project.id} 的评分记录:`, s);
        }
        return match;
    });
    
    // 是否已完成评分（用于显示状态）
    const isCompleted = checkProjectCompleted(project.id);
    if (isCompleted) {
        // 添加状态提示
        $infoContent.append(`
            <div class="info-item">
                <label>状态：</label>
                <span style="color: #52c41a;">已完成评分</span>
            </div>
        `);
    } else {
        // 添加未完成状态提示
        $infoContent.append(`
            <div class="info-item">
                <label>状态：</label>
                <span style="color: #ff4d4f;">待评分</span>
            </div>
        `);
    }

    // 从scoreGroups获取当前阶段的评分项
    let userScoreItems = [];
    
    console.log('开始获取评分项...');
    
    // 优先使用scoreGroups结构
    if (project.scoreGroups && project.scoreGroups[scoreGroupType]) {
        const scoreItems = project.scoreGroups[scoreGroupType];
        console.log(`从scoreGroups中获取${scoreGroupType}阶段评分项:`, scoreItems);
        userScoreItems = scoreItems.filter(item => 
        item.roles && item.roles.includes(userRole)
        );
        console.log(`scoreGroups过滤后用户${username}可评分的项:`, userScoreItems);
    } 
    // 如果没有scoreGroups或当前阶段评分项，尝试使用scoreItems
    else if (Array.isArray(project.scoreItems)) {
        console.log('使用scoreItems:', project.scoreItems);
        userScoreItems = project.scoreItems.filter(item => 
            item.roles && item.roles.includes(userRole)
        );
        console.log(`scoreItems过滤后用户${username}可评分的项:`, userScoreItems);
    }
    
    console.log(`最终过滤后用户${username}可评分的项:`, userScoreItems);
    console.log('过滤条件 - 用户角色:', userRole);
    console.log('过滤条件 - 评分项角色:', userScoreItems.map(item => item.roles));

    // 渲染评分项
    const $scoreItems = $('.score-items');
    $scoreItems.empty();

    if (userScoreItems.length === 0) {
        console.log('没有可评分的项目，显示提示信息');
        $scoreItems.html(`<div class="no-score-items">当前阶段没有您需要评分的项目</div>`);
    } else {
        console.log(`开始渲染${userScoreItems.length}个评分项...`);
        userScoreItems.forEach((item, index) => {
            // 获取之前提交的分数（如果存在）
            let scoreItem = null;
            let score = '';
            // 1. 首先检查草稿中是否有分数 - 修复：草稿键名加入用户名
            const draftKey = `scoreDraft_${username}_${project.id}`;
            const draft = localStorage.getItem(draftKey);
            if (draft) {
                const draftScores = JSON.parse(draft);
                const draftItem = draftScores.find(s => s.itemId === item.id); // 用item.id
                if (draftItem && draftItem.score !== '' && draftItem.score !== null && draftItem.score !== undefined) {
                    score = draftItem.score;
                    console.log(`从草稿中找到用户 ${username} 项目 ${project.id} 的评分项 ${item.id}: ${score}`);
                }
            }
            // 2. 如果草稿中没有分数，再从正式评分记录中获取
            if (score === '' && userScores && Array.isArray(userScores.scores)) {
                scoreItem = userScores.scores.find(s => s.itemId === item.id); // 用item.id
                if (scoreItem && scoreItem.score !== '' && scoreItem.score !== null && scoreItem.score !== undefined) {
                    score = scoreItem.score;
                    console.log(`从正式评分记录中找到项目 ${project.id} 的评分项 ${item.id}: ${score}`);
                }
            }
        // 添加调试信息
            console.log(`评分项${item.id}:`, item);
        console.log(`minScore: ${item.minScore}, maxScore: ${item.maxScore}`);
            console.log(`最终显示分数: ${score}`);
        $scoreItems.append(`
            <div class="score-item">
                <h4>${item.name}<span class="score-range">分值区间：${item.minScore || 0}-${item.maxScore || 100}分</span></h4>
                <div class="score-input">
                    <input type="number" 
                               name="scores[${item.id}]" 
                           min="${item.minScore || 0}" 
                           max="${item.maxScore || 100}" 
                           required 
                               data-item-id="${item.id}"
                               data-item-name="${item.name}"
                               data-min-score="${item.minScore || 0}"
                               data-max-score="${item.maxScore || 100}"
                               value="${score}"
                           placeholder="请输入${item.minScore || 0}-${item.maxScore || 100}的分数"
                           autocomplete="off">
                    <span>分</span>
                </div>
            </div>
        `);
    });
    }

    // 存储当前项目ID
    $('#scoreForm').data('projectId', project.id);

    // 加载评分草稿
    loadScoreDraft();
    
    // 显示提交按钮逻辑
    if (taskType === 1) {
        // 任务类型1：显示提交评分按钮
        $('.btn-submit').show();
        $('#submitScoreBtn').text('提交评分'); // 设置按钮文本为"提交评分"
        $('#completeTaskBtn').hide(); // 确保完成任务按钮隐藏
        $('#scoringForm').removeClass('readonly');
    } else {
        // 任务类型2：检查当前项目是否已完成评分
        const isCurrentProjectCompleted = checkProjectCompleted(project.id);
        const projects = $('#scoreForm').data('projects');
        const allCompleted = Array.isArray(projects) ? projects.every(p => checkProjectCompleted(p.id)) : true;
        
        if (allCompleted) {
            // 所有项目都已完成，只显示完成任务按钮，隐藏提交按钮
            $('.btn-submit').hide();
            $('#completeTaskBtn').show();
        } else if (isCurrentProjectCompleted) {
            // 当前项目已完成，但还有其他未完成项目，只显示下一项目按钮
            $('.btn-submit').show();
            $('#submitScoreBtn').text('下一项目'); // 设置按钮文本为"下一项目"
            $('#completeTaskBtn').hide();
        } else {
            // 当前项目未完成，显示提交按钮
            $('.btn-submit').show();
            $('#submitScoreBtn').text('下一项目'); // 设置按钮文本为"下一项目"
            $('#completeTaskBtn').hide();
        }
        
        $('#scoringForm').removeClass('readonly');
    }

    console.log('=== renderProject 完成 ===');
    console.log('project:', project);
    console.log('当前用户名:', username, '评分项roles:', userScoreItems.map(i => i.roles));
}

function showNoProjectMessage() {
    $('.main-content').html(`
        <div class="project-info">
            <h2>暂无需要评分的项目</h2>
            <p>当前没有需要您评分的项目，请稍后再试。</p>
        </div>
    `);
}

function validateScore(score, minScore = 0, maxScore = 100) {
    // 检查是否是有效数字
    if (isNaN(score)) {
        return { valid: false, message: '请输入有效的分数' };
    }
    
    // 检查范围
    if (score < minScore || score > maxScore) {
        return { valid: false, message: `分数必须在${minScore}-${maxScore}之间` };
    }
    
    return { valid: true };
}

function handleScoreSubmit(e) {
    e.preventDefault();
    disableSubmitBtn();
    showLoading();
    
    const username = auth.getUserInfo().username;
    const projectId = $('#scoreForm').data('projectId');
    const taskType = $('#scoreForm').data('taskType');
    console.log(`处理项目 ${projectId} 的评分提交，任务类型: ${taskType}`);
    
    // 收集所有评分
    const scores = [];
    let hasError = false;

    $('input[type="number"]').each(function() {
        const $input = $(this);
        const inputValue = $input.val();
        
        if (inputValue === '') {
            showError('请填写所有评分项');
            hasError = true;
            return false; // 跳出循环
        }
        
        const score = parseInt(inputValue);
        const minScore = parseInt($input.attr('min')) || 0;
        const maxScore = parseInt($input.attr('max')) || 100;
        
        // 验证分数范围
        if (!validateScore(score, minScore, maxScore)) {
            showError(`评分项分数必须在 ${minScore} 到 ${maxScore} 之间`);
            hasError = true;
            return false; // 跳出循环
        }
        
        scores.push({
            itemId: parseInt($input.data('item-id')), // 这里已是item.id
            score: score
        });
    });
    
    if (hasError) {
        return;
    }

    // 提交评分
    console.log(`提交用户 ${username} 对项目 ${projectId} 的评分:`, scores);
    
    // 构建评分数据对象
    const taskId = $('#scoreForm').data('taskId');
    const scoreData = {
        projectId: projectId,
        taskId: taskId,
        username: username,
        scores: scores,
        totalScore: 0, // 后端会计算总分
        comments: '',
        isDraft: false
    };

    // 同时发送到服务器（模拟），确保服务器先更新
    api.submitScore(scoreData)
        .then(response => {
            if (response.success) {
                console.log('评分提交成功:', response);
                
                // 记录评分到本地存储
                const submittedScore = {
                    projectId: projectId,
                    username: username,
                    scores: scores,
                    submitTime: new Date().toISOString()
                };
                
                // 获取已有的评分记录
                const existingScores = JSON.parse(localStorage.getItem('scores') || '[]');
                
                // 检查是否已经有该项目的评分记录，如果有则替换
                const projectScoreExists = existingScores.some((s, index) => {
                    if (s.projectId === projectId && s.username === username) {
                        existingScores[index] = submittedScore;
                        return true;
                    }
                    return false;
                });
                
                // 如果没有已有记录，则添加新记录
                if (!projectScoreExists) {
                    existingScores.push(submittedScore);
                }
                
                // 保存更新后的记录
                localStorage.setItem('scores', JSON.stringify(existingScores));
                
                // 清除草稿
                const draftKey = `scoreDraft_${username}_${projectId}`;
                localStorage.removeItem(draftKey);
                
                // 再获取一次所有评分记录，确保本地存储和API同步
                api.getScoresByUser(username, taskId)
                    .then(scoresResponse => {
                        if (scoresResponse.success) {
                            // 更新本地存储
                            localStorage.setItem('scores', JSON.stringify(scoresResponse.data));
                            console.log('已同步本地评分记录与API:', scoresResponse.data);
                        }
                        
                        // 继续处理用户界面
                        if (taskType === 1) {
                            // 任务类型1（同步评审）：显示等待消息
                            $('#scoringForm').hide();
                            $('#waitingMessage').show();
                            
                            // 显示当前项目名称
                            const projects = $('#scoreForm').data('projects');
                            const currentProjectIndex = $('#scoreForm').data('currentProjectIndex') || 0;
                            const currentProject = projects[currentProjectIndex];
                            $('#currentProjectName').text(currentProject.name);
                            console.log("完成提交");
                            // 新增：提交后立即拉取一次进度
                            const projectId = $('#scoreForm').data('projectId');
                            const taskId = $('#scoreForm').data('taskId');
                            api.getProjectScoringProgress(projectId, taskId).then(progressRes => {
                                console.log("拉取一次进度", progressRes);
                                if (progressRes.success && progressRes.data && progressRes.data.completed) {
                                    // 如果已完成，直接进入下一个项目
                                    console.log("进入下一个项目");
                                    handleProjectCompletion(projectId);
                                } else {
                                    // 否则进入轮询
                            updateScoringProgress(projectId);
                            startProgressPolling(projectId);
                                }
                            });
                        } else {
                            // 任务类型2：切换到下一个项目
                    switchToNextProject();
                        }
                    })
                    .catch(error => {
                        showError('评分提交出现异常: ' + (error.message || '未知错误'));
                        console.error('提交评分异常:', error);
                    })
                    .finally(() => {
                        enableSubmitBtn();
                        hideLoading();
                    });
                } else {
                showError('评分提交失败: ' + (response.message || '未知错误'));
            }
        })
        .catch(error => {
            showError('评分提交出现异常: ' + (error.message || '未知错误'));
            console.error('提交评分异常:', error);
        })
        .finally(() => {
            enableSubmitBtn();
            hideLoading();
        });
}

// 新增：切换到下一个项目
function switchToNextProject() {
    showLoading();
    const projects = $('#scoreForm').data('projects');
    const currentIndex = $('#scoreForm').data('currentProjectIndex') || 0;
    
    console.log("切换到下一个项目，当前项目索引:", currentIndex);
    console.log("项目列表:", projects);
    
    // 强制提交当前项目的评分到API
    const currentProjectId = $('#scoreForm').data('projectId');
    if (currentProjectId) {
        const scores = [];
        const username = auth.getUserInfo().username;
        let hasUserInput = false;
        
        // 收集当前表单中的评分
        $('input[type="number"]').each(function() {
            const $input = $(this);
            const inputValue = $input.val();
            
            // 检查用户是否实际输入了值（不为空）
            if (inputValue !== '') {
                hasUserInput = true;
            }
            
            const score = parseInt(inputValue);
            // 即使是NaN，也使用0作为默认值保存
            const scoreValue = isNaN(score) ? 0 : score;
            
            scores.push({
                itemId: parseInt($input.data('item-id')),
                score: scoreValue
            });
        });
        
        if (scores.length > 0 && hasUserInput) {
            const taskId = $('#scoreForm').data('taskId');
            const scoreData = {
                projectId: currentProjectId,
                taskId: taskId,
                username: username,
                scores: scores,
                totalScore: 0,
                comments: '',
                isDraft: false
            };
            
            console.log("强制提交当前项目评分到API:", scoreData);
            api.submitScore(scoreData)
                .then(response => {
                    console.log("强制提交评分成功:", response);
                    continueCheckCompletion();
                })
                .catch(error => {
                    console.error("强制提交评分失败:", error);
                    continueCheckCompletion();
                })
                .finally(() => {
                    hideLoading();
                });
        } else {
            continueCheckCompletion();
        }
    } else {
        continueCheckCompletion();
    }
    
    function continueCheckCompletion() {
        // 记录当前项目ID，用于后续检查
        const currentProjId = currentProjectId;
        console.log("当前项目ID:", currentProjId);
        
        // 重新获取API中的评分记录
        const username = auth.getUserInfo().username;
        const userRole = auth.getRole();
        const taskId = $('#scoreForm').data('taskId');
        api.getScoresByUser(username, taskId)
            .then(response => {
                if (response.success && Array.isArray(response.data)) {
                    const apiScores = response.data;
                    console.log("获取到API中的评分记录:", apiScores);
                    
                    // 更新本地存储
                    localStorage.setItem('scores', JSON.stringify(apiScores));
                    console.log("更新本地评分记录:", apiScores);
                }
                
                // 过滤出用户有权限评分的项目
                const userProjects = projects.filter(project => {
                    if (Array.isArray(project.scoreItems)) {
                        return project.scoreItems.some(item => 
                            item.roles && item.roles.includes(userRole)
                        );
                    }
                    if (project.scoreGroups) {
                        const scoreGroupType = $('#scoreForm').data('scoreGroupType') || 'preliminary';
                        const scoreItems = project.scoreGroups[scoreGroupType];
                        if (Array.isArray(scoreItems)) {
                            return scoreItems.some(item => 
                                item.roles && item.roles.includes(userRole)
                            );
                        }
                    }
                    return false;
                });
                
                // 检查是否所有用户有权限的项目都已完成
                let allCompleted = true;
                
                if (Array.isArray(userProjects)) {
                    for (let i = 0; i < userProjects.length; i++) {
                        const completed = checkProjectCompleted(userProjects[i].id);
                        console.log(`项目ID ${userProjects[i].id} 完成状态: ${completed}`);
                        if (!completed) {
                            allCompleted = false;
                            break;
                        }
                    }
                }
                
                console.log("所有用户有权限的项目是否已完成:", allCompleted);
                
                // 检查当前项目在用户项目列表中的位置
                const userProjectIndex = userProjects.findIndex(p => p.id === currentProjectId);
                const isLastUserProject = userProjectIndex === userProjects.length - 1;
                console.log("是否是用户最后一个项目:", isLastUserProject);
                
                // 检查当前项目是否已完成评分
                const currentProjectCompleted = checkProjectCompleted(currentProjectId);
                console.log("当前项目是否已完成:", currentProjectCompleted);
                
                // 如果是用户最后一个项目，并且当前项目已完成评分，弹出确认框
                if (isLastUserProject && currentProjectCompleted) {
                    console.log("用户最后一个项目完成，弹出确认框");
                    showTaskCompletionDialog();
                    return;
                } else if (allCompleted) {
                    console.log("所有用户有权限的项目都已完成，弹出确认框");
                    showTaskCompletionDialog();
                    return;
                }
    
                // 找到下一个未完成的项目（在用户有权限的项目中）
                console.log("查找下一个未完成的项目");
                for (let i = userProjectIndex + 1; i < userProjects.length; i++) {
                    if (!checkProjectCompleted(userProjects[i].id)) {
                        const originalIndex = projects.findIndex(p => p.id === userProjects[i].id);
                        console.log(`找到下一个未完成项目，索引: ${originalIndex}, ID: ${userProjects[i].id}`);
                        switchToProject(originalIndex);
            return;
        }
    }
    
    // 如果后面没有未完成的项目，检查前面是否有
                console.log("后面没有未完成的项目，检查前面是否有");
                for (let i = 0; i < userProjectIndex; i++) {
                    if (!checkProjectCompleted(userProjects[i].id)) {
                        const originalIndex = projects.findIndex(p => p.id === userProjects[i].id);
                        console.log(`找到前面未完成项目，索引: ${originalIndex}, ID: ${userProjects[i].id}`);
                        switchToProject(originalIndex);
            return;
        }
    }
    
                // 如果代码执行到这里，说明所有项目都已完成，但allCompleted为false
                console.log("异常情况：单独检查每个项目都已完成，但allCompleted为false");
                showTaskCompletionDialog();
            })
            .catch(error => {
                console.error("获取评分记录失败:", error);
                // 继续使用本地数据检查完成状态
                checkCompletionWithLocalData();
            })
            .finally(() => {
                hideLoading();
            });
    }
    
    function checkCompletionWithLocalData() {
        // 记录当前项目ID，用于后续检查
        const currentProjId = currentProjectId;
        console.log("当前项目ID(本地数据):", currentProjId);
        
        // 过滤出用户有权限评分的项目
        const username = auth.getUserInfo().username;
        const userRole = auth.getRole();
        const userProjects = projects.filter(project => {
            if (Array.isArray(project.scoreItems)) {
                return project.scoreItems.some(item => 
                    item.roles && item.roles.includes(userRole)
                );
            }
            if (project.scoreGroups) {
                const scoreGroupType = $('#scoreForm').data('scoreGroupType') || 'preliminary';
                const scoreItems = project.scoreGroups[scoreGroupType];
                if (Array.isArray(scoreItems)) {
                    return scoreItems.some(item => 
                        item.roles && item.roles.includes(userRole)
                    );
                }
            }
            return false;
        });
        
        // 检查是否所有用户有权限的项目都已完成
        let allCompleted = true;
        
        if (Array.isArray(userProjects)) {
            for (let i = 0; i < userProjects.length; i++) {
                const completed = checkProjectCompleted(userProjects[i].id);
                console.log(`项目ID ${userProjects[i].id} 完成状态: ${completed}`);
                if (!completed) {
                    allCompleted = false;
                    break;
                }
            }
        }
        
        console.log("所有用户有权限的项目是否已完成(本地数据):", allCompleted);
        
        // 检查当前项目在用户项目列表中的位置
        const userProjectIndex = userProjects.findIndex(p => p.id === currentProjId);
        const isLastUserProject = userProjectIndex === userProjects.length - 1;
        console.log("是否是用户最后一个项目(本地数据):", isLastUserProject);
        
        // 检查当前项目是否已完成评分
        const currentProjectCompleted = checkProjectCompleted(currentProjId);
        console.log("当前项目是否已完成(本地数据):", currentProjectCompleted);
        
        // 如果是用户最后一个项目，并且当前项目已完成评分，弹出确认框
        if (isLastUserProject && currentProjectCompleted) {
            console.log("用户最后一个项目完成，弹出确认框");
            showTaskCompletionDialog();
            return;
        } else if (allCompleted) {
            console.log("所有用户有权限的项目都已完成，弹出确认框");
            showTaskCompletionDialog();
            return;
        }
        
        // 找到下一个未完成的项目（在用户有权限的项目中）
        console.log("查找下一个未完成的项目");
        for (let i = userProjectIndex + 1; i < userProjects.length; i++) {
            if (!checkProjectCompleted(userProjects[i].id)) {
                const originalIndex = projects.findIndex(p => p.id === userProjects[i].id);
                console.log(`找到下一个未完成项目，索引: ${originalIndex}, ID: ${userProjects[i].id}`);
                switchToProject(originalIndex);
                return;
            }
        }
        
        // 如果后面没有未完成的项目，检查前面是否有
        console.log("后面没有未完成的项目，检查前面是否有");
        for (let i = 0; i < userProjectIndex; i++) {
            if (!checkProjectCompleted(userProjects[i].id)) {
                const originalIndex = projects.findIndex(p => p.id === userProjects[i].id);
                console.log(`找到前面未完成项目，索引: ${originalIndex}, ID: ${userProjects[i].id}`);
                switchToProject(originalIndex);
                return;
            }
        }
        
        // 如果代码执行到这里，说明所有项目都已完成，但allCompleted为false
        console.log("异常情况：单独检查每个项目都已完成，但allCompleted为false");
        showTaskCompletionDialog();
    }
    hideLoading();
}

function showWaitingMessage(projectId) {
    $('#scoringForm').hide();
    $('#waitingMessage').show();
    updateScoringProgress(projectId);
}

function updateScoringProgress(projectId) {
    // 先检查mockApi是否已定义
    if (!api || typeof api.getProjectScoringProgress !== 'function') {
        console.error('api或getProjectScoringProgress函数未定义');
        return;
    }
    
    // 显示等待消息区域
    $('#waitingMessage').show();
    $('#scoringForm').hide();
    
    // 获取最新评分记录后再更新进度
    api.getProjectScores(projectId, $('#scoreForm').data('taskId'))
        .then(response => {
            if (response.success) {
                // 根据API返回的数据结构，提取评分记录
                let apiScores = [];
                if (response.data) {
                    // 如果是mock API，评分记录在 response.data.details 中
                    if (response.data.details && Array.isArray(response.data.details)) {
                        apiScores = response.data.details;
                    }
                    // 如果是后端API，评分记录在 response.data 中（需要进一步处理）
                    else if (response.data.statistics) {
                        // 后端API返回的是项目详情，不包含评分记录
                        apiScores = [];
                    }
                }
                
                // 更新本地存储
                localStorage.setItem('scores', JSON.stringify(apiScores));
                console.log('更新评分进度前已同步本地评分记录与API:', apiScores);
                
                // 获取进度
                const taskId = $('#scoreForm').data('taskId');
                return api.getProjectScoringProgress(projectId, taskId);
            } else {
                console.error('获取评分记录失败');
                return Promise.reject('获取评分记录失败');
            }
        })
        .then(response => {
            if (response.success) {
                const progress = response.data;
                console.log(`项目${projectId}进度更新详情:`, progress);
                
                // 更新进度显示 - 修复：统一使用后端API字段名
                const scoredCount = progress.completedExperts || progress.scored || 0;
                const totalCount = progress.totalExperts || progress.total || 0;
                $('#scoringProgress').text(`${scoredCount}/${totalCount}`);
                
                // 更新项目名称显示
                if (progress.projectName) {
                    $('#currentProjectName').text(progress.projectName);
                }
                
                // 更新已评分专家列表显示
                if (progress.scoredExperts && progress.scoredExperts.length > 0) {
                    // 如果不存在则创建元素
                    if ($('#scoredExperts').length === 0) {
                        $('#waitingMessage').append(`
                            <p>已评分专家: <span id="scoredExperts"></span></p>
                        `);
                    }
                    $('#scoredExperts').text(progress.scoredExperts.join(', '));
                }
                
                // 如果所有专家都已完成评分，进入下一个项目
                if (progress.completed) {
                    console.log(`项目${projectId}所有专家评分已完成，准备加载下一个项目`);
                    
                    // 立即执行一次项目评分完成后的处理逻辑
                    handleProjectCompletion(projectId);
                } else {
                    // 确保等待消息区域可见
                    $('#waitingMessage').show();
                    $('#scoringForm').hide();
                }
            } else {
                console.error('获取评分进度失败:', response.message);
                // 显示错误提示
                showError('获取评分进度失败：' + (response.message || '未知错误'));
            }
        })
        .catch(error => {
            console.error('获取评分进度出错:', error);
            // 显示错误提示
            showError('获取评分进度失败：' + (error.message || '未知错误'));
        });
}

function startProgressPolling(projectId) {
    console.log(`启动项目${projectId}评分进度轮询`);
    
    // 确保不会创建多个轮询
    if (window.progressPollingInterval) {
        clearInterval(window.progressPollingInterval);
        window.progressPollingInterval = null;
    }
    
    // 隐藏刷新按钮区域（如果存在）
    $('#refreshSection').hide();
    
    // 首次更新评分进度
    updateScoringProgress(projectId);
    
    // 创建轮询，减少轮询间隔到1秒，加快进度更新
    window.progressPollingInterval = setInterval(() => {
        console.log(`轮询检查项目${projectId}评分进度`);
        
        // 每次轮询前先重新获取最新评分记录，确保数据同步
        api.getProjectScores(projectId, $('#scoreForm').data('taskId'))
            .then(response => {
                if (response.success) {
                    // 根据API返回的数据结构，提取评分记录
                    let apiScores = [];
                    if (response.data) {
                        // 如果是mock API，评分记录在 response.data.details 中
                        if (response.data.details && Array.isArray(response.data.details)) {
                            apiScores = response.data.details;
                        }
                        // 如果是后端API，评分记录在 response.data 中（需要进一步处理）
                        else if (response.data.statistics) {
                            // 后端API返回的是项目详情，不包含评分记录
                            apiScores = [];
                        }
                    }
                    
                    // 更新本地存储
                    localStorage.setItem('scores', JSON.stringify(apiScores));
                    console.log('轮询中已同步本地评分记录与API');
                    
                    // 获取进度
                    const taskId = $('#scoreForm').data('taskId');
                    return api.getProjectScoringProgress(projectId, taskId);
                } else {
                    console.error('获取评分记录失败');
                    return Promise.reject('获取评分记录失败');
                }
            })
            .then(response => {
                console.log(`项目${projectId}评分进度响应:`, response);
                
                if (response.success) {
                    const progress = response.data;
                    
                    // 更新UI显示
                    $('#scoringProgress').text(`${progress.completedExperts}/${progress.totalExperts}`);
                    
                    console.log(`项目${projectId}评分进度:`, progress);
                    
                    // 添加当前评分专家和总专家详情
                    console.log(`已评分专家列表:`, progress.scoredExperts);
                    console.log(`评分完成状态:`, progress.completed ? "已完成" : "未完成");
                    
                    if (progress.completed) {
                        console.log(`项目${projectId}所有专家评分已完成，准备加载下一个项目`);
                        
                        // 立即执行一次项目评分完成后的处理逻辑
                        handleProjectCompletion(projectId);
                    }
                } else {
                    console.error(`获取项目${projectId}评分进度失败:`, response.message);
                }
            })
            .catch(error => {
                console.error(`获取项目${projectId}评分进度失败:`, error);
            });
    }, 1000); // 减少为1秒轮询一次
    
    // 创建一个安全检查，确保轮询不会超过30秒（避免无限等待）
    setTimeout(() => {
        if (window.progressPollingInterval) {
            console.log(`项目${projectId}评分等待超时，强制检查进度`);
            
            // 再次检查进度
            const taskId = $('#scoreForm').data('taskId');
        api.getProjectScoringProgress(projectId, taskId)
            .then(response => {
                if (response.success) {
                    const progress = response.data;
                        console.log(`强制检查进度结果:`, progress);
                    
                    if (progress.completed) {
                            console.log(`项目${projectId}评分已完成，进行处理`);
                            handleProjectCompletion(projectId);
                        } else {
                            // 如果30秒后仍未完成，显示刷新按钮区域
                            $('#refreshSection').show();
                            
                            // 绑定刷新按钮点击事件
                            $('#refreshButton').off('click').on('click', function() {
                                console.log('用户手动刷新进度');
                                // 隐藏刷新按钮区域
                                $('#refreshSection').hide();
                                // 重新获取评分进度
                                updateScoringProgress(projectId);
                            });
                    }
                }
            });
        }
    }, 30000); // 30秒后进行安全检查
}

// 添加一个单独的函数来处理项目评分完成后的逻辑
function handleProjectCompletion(projectId) {
    if (window.progressPollingInterval) {
        clearInterval(window.progressPollingInterval);
        window.progressPollingInterval = null;
    }
    $('#waitingMessage').hide();
    $('#refreshSection').hide();
    
    // 修复：等待loadCurrentTask完全完成后再渲染项目
    loadCurrentTask().then(() => {
        // 获取最新的 projects 和当前索引
        const projects = $('#scoreForm').data('projects');
        const currentIndex = $('#scoreForm').data('currentProjectIndex') || 0;
        if (projects && projects[currentIndex]) {
            // 确保项目数据完整后再渲染
            const project = projects[currentIndex];
            console.log('handleProjectCompletion - 准备渲染项目:', project);
            
            // 检查项目数据是否完整
            if (project.scoreItems && Array.isArray(project.scoreItems) && project.scoreItems.length > 0) {
                console.log('项目数据完整，直接渲染');
                renderProject(project);
            } else {
                console.log('项目数据不完整，等待数据加载完成');
                // 如果项目数据不完整，等待一段时间后重试
                setTimeout(() => {
                    const updatedProjects = $('#scoreForm').data('projects');
                    const updatedProject = updatedProjects[currentIndex];
                    if (updatedProject && updatedProject.scoreItems && Array.isArray(updatedProject.scoreItems)) {
                        console.log('数据加载完成，渲染项目:', updatedProject);
                        renderProject(updatedProject);
                    } else {
                        console.error('项目数据仍然不完整，强制刷新页面');
                        location.reload();
                    }
                }, 500);
            }
        }
    }).catch(error => {
        console.error('handleProjectCompletion 失败:', error);
        // 如果失败，刷新页面
        location.reload();
    });
}

function showScoringHistory() {
    const projectId = $('#scoreForm').data('projectId');
    const username = auth.getUserInfo().username;
    
    api.getScoringHistory(projectId, username)
        .then(response => {
            if (response.success) {
                renderScoringHistory(response.data);
            }
        });
}

function renderScoringHistory(history) {
    const $history = $('<div class="scoring-history">');
    history.forEach(record => {
        $history.append(`
            <div class="history-item">
                <span class="item-name">${record.itemName}</span>
                <span class="score">${record.score}分</span>
                <span class="time">${new Date(record.submitTime).toLocaleString()}</span>
            </div>
        `);
    });
    
    $('.score-items').after($history);
}

// 自动保存相关函数
function initAutoSave() {
    let autoSaveTimer;
    
    $('input[type="number"]').on('input', function() {
        clearTimeout(autoSaveTimer);
        autoSaveTimer = setTimeout(saveScoreDraft, 1000);
    });
}

function saveScoreDraft() {
    const projectId = $('#scoreForm').data('projectId');
    const username = auth.getUserInfo().username;
    const scores = [];
    let hasInput = false;
    
    $('input[type="number"]').each(function() {
        const $input = $(this);
        const inputValue = $input.val();
        if (inputValue !== '') {
            hasInput = true;
        scores.push({
                itemId: parseInt($input.data('item-id')),
                score: parseInt(inputValue)
        });
        }
    });
    
    const draftKey = `scoreDraft_${username}_${projectId}`;
    if (hasInput) {
    localStorage.setItem(draftKey, JSON.stringify(scores));
    } else {
        localStorage.removeItem(draftKey);
    }
}

function loadScoreDraft() {
    const projectId = $('#scoreForm').data('projectId');
    const username = auth.getUserInfo().username;
    
    // 修复：草稿键名加入用户名，确保不同用户的草稿互不影响
    const draftKey = `scoreDraft_${username}_${projectId}`;
    const draft = localStorage.getItem(draftKey);
    
    if (draft) {
        const scores = JSON.parse(draft);
        scores.forEach(item => {
            // 修复：始终从草稿中加载值，即使输入框已有值
            const $input = $(`input[data-item-id="${item.itemId}"]`); // 用itemId
            if ($input.length > 0) {
                $input.val(item.score);
                console.log(`从草稿加载用户 ${username} 的评分: itemId=${item.itemId}, score=${item.score}`);
            }
        });
    }
}

// 添加错误提示函数
function showError(message) {
    const $error = $('.error-message');
    if ($error.length === 0) {
        $('<div class="error-message"></div>').insertAfter('#scoreForm');
    }
    $('.error-message').text(message).fadeIn();
}

// 新增：加载任务并显示最后一个项目（从"否"按钮返回时调用）
function loadTaskWithLastProject() {
    showLoading();
    const username = auth.getUserInfo().username;
    
    // 首先获取API中的评分记录，确保本地数据与API数据同步
    api.getScoresByUser(username, $('#scoreForm').data('taskId'))
        .then(scoresResponse => {
            // 获取API评分记录
            const apiScores = scoresResponse.success && Array.isArray(scoresResponse.data) ? 
                scoresResponse.data : [];
            console.log('API评分记录:', apiScores);
            
            // 获取本地存储的评分记录
            const localScores = JSON.parse(localStorage.getItem('scores') || '[]');
            console.log('本地评分记录:', localScores);
            
            // 合并API和本地评分记录
            const mergedScores = [...apiScores];
            localScores.forEach(localScore => {
                const exists = mergedScores.some(s => 
                    s.projectId === localScore.projectId && s.username === localScore.username
                );
                if (!exists) {
                    mergedScores.push(localScore);
                }
            });
            
            // 保存合并后的评分记录到localStorage
            localStorage.setItem('scores', JSON.stringify(mergedScores));
            console.log('合并后的评分记录:', mergedScores);
            
            // 然后加载任务和项目
            api.getActiveTaskWithProjects(username)
                .then(response => {
                    if (response.success && response.data) {
                        const { task, projectsInOrder } = response.data;
                        
                        if (!task || !Array.isArray(projectsInOrder) || projectsInOrder.length === 0) {
                            // 没有任务或项目，回到正常流程
                            loadCurrentTask();
                            return;
                        }
                        
                        // 有任务且有项目，显示任务名称
                        $('#currentTaskName').remove();
                        $('.main-content').html(`
                            <div id="currentTaskName" style="font-size:18px;font-weight:bold;margin-bottom:16px;">当前评审任务：${task.category || task.id}</div>
                            <div id="projectNavigation" style="margin-bottom: 20px; display: block;">
                                <h4>项目导航</h4>
                                <div class="project-icons" id="projectIcons"></div>
                            </div>
                            <div class="project-info" id="projectInfo">
                                <h2>当前评审项目</h2>
                                <div class="info-content"></div>
                            </div>
                            <div class="scoring-form" id="scoringForm">
                                <h3>评分表</h3>
                                <form id="scoreForm">
                                    <div class="score-items"></div>
                                    <div class="form-actions">
                                        <button type="submit" class="btn btn-submit">提交评分</button>
                                    </div>
                                </form>
                            </div>
                            <div class="form-actions" style="margin-top: 20px;">
                                <button type="button" id="completeTaskBtn" class="btn btn-complete-task">完成评审任务</button>
                            </div>
                        `);
                        
                        // 存储任务信息
                        $('#scoreForm').data('taskId', task.id);
                        $('#scoreForm').data('taskType', task.taskType);
                        $('#scoreForm').data('scoreGroupType', task.scoreGroupType); // 保存评分组类型
                        $('#scoreForm').data('projects', projectsInOrder);
                        
                        // 输出任务信息，便于调试
                        console.log('当前任务信息:', {
                            id: task.id,
                            taskType: task.taskType,
                            scoreGroupType: task.scoreGroupType,
                            projectCount: projectsInOrder.length
                        });
                        
                        // 重新绑定事件
                        $('#scoreForm').on('submit', handleScoreSubmit);
                        $('#completeTaskBtn').on('click', showTaskCompletionDialog);
                        
                        // 渲染项目导航
                        renderProjectNavigation(projectsInOrder);
                        
                        // 直接显示最后一个项目
                        const lastIndex = projectsInOrder.length - 1;
                        $('#scoreForm').data('currentProjectIndex', lastIndex);
                        switchToProject(lastIndex);

                        // 强制重新读取评分数据并刷新导航栏
                        setTimeout(() => {
                            const mergedScores = JSON.parse(localStorage.getItem('scores') || '[]');
                            console.log('刷新前评分数据:', mergedScores);
                            refreshAllProjectNavigation();
                        }, 0);
                    } else {
                        // 无任务，回到正常流程
                        loadCurrentTask();
                    }
                })
                .catch(error => {
                    console.error('加载任务失败:', error);
                    showError('加载任务失败，请刷新页面重试');
                    loadCurrentTask(); // 回退到正常流程
                })
                .finally(() => {
                    hideLoading();
                });
        })
        .catch(error => {
            console.error('加载评分记录失败:', error);
            loadCurrentTask(); // 回退到正常流程
        })
        .finally(() => {
            hideLoading();
        });
}

// 新增：刷新任务状态函数
function refreshTaskStatus() {
    showLoading();
    const username = auth.getUserInfo().username;
    const taskId = $('#scoreForm').data('taskId') || localStorage.getItem('latestReviewTaskId');
    
    console.log("刷新任务状态，任务ID:", taskId);
    
    if (!taskId) {
        console.error("无法获取任务ID，重新加载页面");
        location.reload();
        return;
    }
    
    // 显示加载提示
    $('.main-content').html(`
        <div class="project-info">
            <h2>正在刷新任务状态...</h2>
            <p>请稍候，正在获取最新的评审进度。</p>
        </div>
    `);
    
    // 重新获取任务完成状态
    api.checkTaskCompletionStatus(taskId)
        .then(response => {
            if (response.success) {
                const status = response.data;
                console.log("刷新后的任务完成状态:", status);
                
                if (status.isCompleted) {
                    // 任务已完成，跳转到总结页面
                    console.log("任务已完成，跳转到总结页面");
                    window.location.href = 'review-complete.html';
                } else {
                    // 任务未完成，重新加载任务和项目
                    console.log("任务未完成，重新加载任务和项目");
                    return loadCurrentTask();
                }
            } else {
                throw new Error(response.message || '获取任务状态失败');
            }
        })
        .catch(error => {
            console.error("刷新任务状态失败:", error);
            // 如果刷新失败，重新加载页面
            location.reload();
        })
        .finally(() => {
            hideLoading();
        });
}

// 新增批量刷新函数
function refreshAllProjectNavigation() {
    const projects = $('#scoreForm').data('projects');
    const currentIndex = $('#scoreForm').data('currentProjectIndex');
    if (Array.isArray(projects) && typeof currentIndex === 'number') {
        updateProjectNavigation(projects, currentIndex);
        checkAllProjectsCompleted();
    }
}

// ====== 全局加载遮罩工具函数 ======
function showLoading() {
    $('.loading-overlay').show();
}
function hideLoading() {
    $('.loading-overlay').hide();
}

// ====== 提交按钮防重复工具 ======
function disableSubmitBtn() {
    $('#submitScoreBtn').prop('disabled', true);
}
function enableSubmitBtn() {
    $('#submitScoreBtn').prop('disabled', false);
}
