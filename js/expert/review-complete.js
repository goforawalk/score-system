// 版本号：v1.2 - 修复评分阶段显示为中文，移除总分和平均分显示，添加用户验证和初始化功能
$(document).ready(function() {
    // 验证用户权限 - 检查角色是否以'expert'开头，支持expert1、expert2等
    if (!auth.isLoggedIn() || !auth.getRole().startsWith('expert')) {
        window.location.href = '../index.html';
        return;
    }

    // 初始化用户信息
    const userInfo = auth.getUserInfo();
    $('#currentUser').text(userInfo.username);
    
    // 绑定退出按钮事件
    $('#logoutBtn').on('click', function(e) {
        e.preventDefault();
        auth.removeUserInfo();
        window.location.href = '../index.html';
    });

    const username = userInfo.username;
    const taskId = localStorage.getItem('latestReviewTaskId');

    if (!taskId || !username) {
        showNoTask();
        return;
    }

    console.log("尝试获取评审任务，任务ID:", taskId);

    // 获取任务详情
    api.getReviewTask(taskId)
        .then(taskRes => {
        if (!taskRes.success || !taskRes.data) {
                console.error("获取任务详情失败:", taskRes);
            showNoTask();
            return;
        }
        const task = taskRes.data;
            console.log("获取到任务详情:", task);
            
        // 通过API批量获取所有项目详情
        const projectIds = task.projectIdsInOrder || [];
        const projectDetailPromises = projectIds.map(pid => api.getProjectDetails(pid, taskId));
        Promise.all(projectDetailPromises).then(projectResults => {
            const projects = projectResults.map(res => res.success && res.data ? res.data : null).filter(Boolean);
            if (projects.length === 0) {
                console.warn("任务中没有找到有效项目");
                showNoTask("此评审任务中没有有效项目");
                return;
            }
            // 获取评分记录
            api.getScoresByUser(username, taskId)
                .then(scoreRes => {
                    if (!scoreRes.success) {
                        console.error("获取评分记录失败:", scoreRes);
                        showNoTask("获取评分记录失败");
                        return;
                    }
                    const allScores = Array.isArray(scoreRes.data) ? scoreRes.data : [];
                    console.log("获取到所有评分记录:", allScores);
                    // 只取当前专家本任务下的评分
                    const myScores = allScores.filter(s => 
                        s.username === username && projectIds.includes(s.projectId)
                    );
                    console.log("当前用户在本任务下的评分记录:", myScores);

                    // 如果API中没有找到评分记录，尝试从本地存储中获取
                    if (myScores.length === 0) {
                        console.warn("API中没有找到评分记录，尝试从本地存储中获取");
                        const localScores = JSON.parse(localStorage.getItem('scores') || '[]');
                        const localMyScores = localScores.filter(s => 
                            s.username === username && projectIds.includes(s.projectId)
                        );
                        
                        if (localMyScores.length > 0) {
                            console.log("从本地存储中找到评分记录:", localMyScores);
                            renderSummary(task, projects, localMyScores);
                            return;
                        }
                        
                        // 如果本地存储也没有，尝试从草稿中获取
                        console.warn("本地存储中也没有找到评分记录，尝试从草稿中获取");
                        const draftScores = [];
                        
                        projects.forEach(project => {
                            const draftKey = `scoreDraft_${username}_${project.id}`;
                            const draft = localStorage.getItem(draftKey);
                            
                            if (draft) {
                                try {
                                    const scores = JSON.parse(draft);
                                    if (Array.isArray(scores) && scores.length > 0) {
                                        const hasValidScore = scores.some(item => item.score > 0);
                                        if (hasValidScore) {
                                            draftScores.push({
                                                projectId: project.id,
                                                username: username,
                                                scores: scores,
                                                submitTime: new Date().toISOString()
                                            });
                                        }
                                    }
                                } catch (e) {
                                    console.error(`解析项目 ${project.id} 的草稿数据失败:`, e);
                                }
                            }
                        });
                        
                        if (draftScores.length > 0) {
                            console.log("从草稿中构建评分记录:", draftScores);
                            renderSummary(task, projects, draftScores);
                            return;
                        }
                    }

            renderSummary(task, projects, myScores);
                })
                .catch(error => {
                    console.error("获取评分记录时发生错误:", error);
                    showNoTask("获取评分记录时发生错误");
                });
        });
    })
    .catch(error => {
        console.error("获取任务详情时发生错误:", error);
        showNoTask("获取任务详情时发生错误");
    });

    function showNoTask(message = "暂无可展示的评审任务") {
        $('.main-content').html(`<div class="project-info"><h2>${message}</h2></div>`);
    }

    function renderSummary(task, projects, myScores) {
        console.log("渲染评审任务总结，任务:", task);
        console.log("项目列表:", projects);
        console.log("我的评分记录:", myScores);
        
        // 将评分阶段转换为中文
        const scoreGroupText = {
            'preliminary': '初赛',
            'semifinal': '复赛',
            'final': '决赛'
        }[task.scoreGroupType] || '未知';
        
        let html = `<div class="project-info">
            <h2>评审任务总结</h2>
            <div><b>任务名称：</b>${task.category || task.id}</div>
            <div><b>任务类型：</b>${task.taskType === 1 ? '全部专家完成后进入下一项目' : '个人完成后可进入下一项目'}</div>
            <div><b>评分阶段：</b>${scoreGroupText}</div>
            <div><b>完成时间：</b>${task.completedAt ? new Date(task.completedAt).toLocaleString() : '-'}</div>
            <hr>
            <h3>项目评分详情</h3>
        `;

        let totalCount = 0;
        projects.forEach(project => {
            html += `<div style="margin-bottom:16px;"><b>项目：</b>${project.name}</div>`;
            const scoreRecord = myScores.find(s => s.projectId === project.id);
            
            if (scoreRecord && Array.isArray(scoreRecord.scores) && scoreRecord.scores.length > 0) {
                console.log(`项目 ${project.id} 的评分记录:`, scoreRecord);
                scoreRecord.scores.forEach((item, idx) => {
                    // 尝试获取评分项名称
                    let itemName = `评分项${idx + 1}`;
                    
                    // 从项目的scoreGroups中获取评分项名称
                    if (project.scoreGroups && project.scoreGroups[task.scoreGroupType]) {
                        const scoreItems = project.scoreGroups[task.scoreGroupType];
                        if (Array.isArray(scoreItems) && scoreItems[idx]) {
                            itemName = scoreItems[idx].name || itemName;
                        }
                    }
                    
                    html += `<div style="margin-left:24px;">${itemName}：${item.score}分</div>`;
                    totalCount++;
                });
            } else {
                console.log(`项目 ${project.id} 没有评分记录`);
                
                // 如果没有评分记录，显示评分项但标记为未评分
                if (project.scoreGroups && project.scoreGroups[task.scoreGroupType]) {
                    const scoreItems = project.scoreGroups[task.scoreGroupType];
                    if (Array.isArray(scoreItems) && scoreItems.length > 0) {
                        scoreItems.forEach(item => {
                            html += `<div style="margin-left:24px;">${item.name}：<span style="color:#ff4d4f;">未评分</span></div>`;
                        });
                    } else {
                        html += `<div style="margin-left:24px;">无评分记录</div>`;
                    }
            } else {
                html += `<div style="margin-left:24px;">无评分记录</div>`;
                }
            }
        });

        // 移除总分和平均分的显示
        html += `<hr><div><b>评审项目数：</b>${totalCount > 0 ? projects.length : 0}</div>`;
        html += `<div style="margin-top:24px;">
            <!--button class="btn" id="backToScoring">返回评分界面</button>
            <button class="btn" id="logoutBtn">退出系统</button-->
        </div></div>`;

        $('.main-content').html(html);

        $('#backToScoring').on('click', function() {
            window.location.href = 'scoring.html';
        });
        $('#logoutBtn').on('click', function() {
            // 添加确认框询问是否清空缓存
            if (confirm('是否清空本地缓存数据？\n选择"确定"将清空所有缓存，避免影响下一次测试。\n选择"取消"将保留缓存数据。')) {
                // 清空所有相关缓存数据
                localStorage.removeItem('scores');
                localStorage.removeItem('latestReviewTaskId');
                
                // 清除所有评分草稿 - 不仅仅是当前用户的
                const keysToRemove = [];
                for (let i = 0; i < localStorage.length; i++) {
                    const key = localStorage.key(i);
                    if (key && key.startsWith('scoreDraft_')) {
                        keysToRemove.push(key);
                    }
                }
                
                // 单独循环删除，避免在遍历过程中修改localStorage导致索引错乱
                keysToRemove.forEach(key => {
                    localStorage.removeItem(key);
                });
                
                // 清除API中的评分记录
                if (typeof mockApi !== 'undefined') {
                    mockApi.scores = [];
                    console.log('已清空API中的评分记录');
                }
                
                console.log('已清空所有缓存数据');
            } else {
                console.log('保留缓存数据');
            }
            
            localStorage.removeItem('userInfo');
            window.location.href = '../index.html';
        });
    }
});
