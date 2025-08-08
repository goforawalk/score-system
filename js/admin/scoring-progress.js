$(document).ready(function() {
    // 验证用户权限
    if (!auth.isLoggedIn() || !auth.isAdmin()) {
        console.log('权限验证失败 - 用户角色:', auth.getRole());
        window.location.href = '../index.html';
        return;
    }

    $('#exportBtn').hide(); // 默认隐藏
    $('#refreshBtn').hide(); // 默认隐藏刷新按钮

    // 初始化用户信息
    initializeUserInfo();
    
    // 初始化页面
    loadTaskList();
});

let timer = null;
let countdown = 30;
let currentTaskId = null;

// 初始化用户信息
function initializeUserInfo() {
    const userInfo = auth.getUserInfo();
    $('#currentUser').text(userInfo.name || userInfo.username);
    $('#logoutBtn').on('click', function(e) {
        e.preventDefault();
        logoutWithCacheConfirm();
    });
}

// 退出登录并确认缓存清理
function logoutWithCacheConfirm() {
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
}

// 彻底清理所有评分相关缓存
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

// 倒计时显示
function updateCountdownDisplay() {
    document.getElementById('countdown').textContent = countdown;
}

// 启动定时刷新
function startAutoRefresh(taskId) {
    if (timer) clearInterval(timer);
    countdown = 30;
    updateCountdownDisplay();
    currentTaskId = taskId;

    $('#refreshBtn').show(); // 自动刷新时显示刷新按钮

    timer = setInterval(() => {
        countdown--;
        updateCountdownDisplay();
        if (countdown <= 0) {
            loadProgress(currentTaskId, true); // 自动刷新
        }
    }, 1000);
}

// 停止自动刷新时隐藏刷新按钮
function stopAutoRefresh() {
    if (timer) clearInterval(timer);
    $('#refreshBtn').hide();
}

// 1. 加载任务列表（只显示启动中和已完成的任务）
function loadTaskList() {
    showLoading();
    api.getSimpleTasks()
        .then(response => {
            if (response.success) {
                const $select = $('#taskSelect');
                $select.find('option:not(:first)').remove();
                // 只显示状态为“启动中”或“已完成”的任务
                response.data.filter(task => task.status === 'active' || task.status === 'completed').forEach(task => {
                    const statusText = task.status === 'active' ? '(进行中)' : '(已完成)';
                    $select.append(`
                        <option value="${task.id}">${task.category} ${statusText}</option>
                    `);
                });
                if ($select.find('option').length === 1) {
                    $select.append('<option disabled>暂无可用评审任务</option>');
                }
            }
        })
        .catch(error => {
            showError('加载任务列表失败');
            console.error(error);
        })
        .finally(() => {
            hideLoading();
        });
}

// 2. 加载进度数据
function loadProgress(taskId, isAuto) {
    showLoading();
    Promise.all([
        api.getTaskProjectProgressAndScores(taskId),
        api.getReviewTask(taskId)
    ]).then(([progressResponse, taskResponse]) => {
        if (progressResponse.success && taskResponse.success) {
            const tbody = document.getElementById('userList');
            tbody.innerHTML = '';
            const data = progressResponse.data;
            window.currentProjectList = data; // 确保全局赋值

            // 判断是否是同步评审任务且为手动切换模式
            const isSyncTask = taskResponse.data && taskResponse.data.taskType === 1;
            const isManualSwitch = taskResponse.data && taskResponse.data.switchMode === 2;
            const isCompleted = taskResponse.data && taskResponse.data.status === 'completed';

            // 如果任务已完成，停止自动刷新，隐藏切换模式区域
            if (isCompleted) {
                stopAutoRefresh();
                $('#switchModeInfo').hide();
            }

            // 找到第一个未评审的项目
            let firstUnreviewedFound = false;
            // 更新项目列表
            data.forEach(item => {
                const tr = document.createElement('tr');
                // 只有在同步评审且手动切换模式下，才考虑高亮显示
                if (isSyncTask && isManualSwitch && !firstUnreviewedFound && 
                    (item.isReviewed === 0 || item.is_reviewed === 0)) {
                    tr.className = 'highlight-row';
                    firstUnreviewedFound = true; // 标记已找到第一个未评审项目
                }
                // 项目名称
                const tdName = document.createElement('td');
                tdName.textContent = item.projectName;
                tr.appendChild(tdName);
                // 评分进度
                const tdProgress = document.createElement('td');
                tdProgress.innerHTML = `${item.completedExperts} / ${item.totalExperts} (${item.completionRate ? item.completionRate.toFixed(1) : 0}%)`;
                tr.appendChild(tdProgress);
                // 进度条
                const tdBar = document.createElement('td');
                const barContainer = document.createElement('div');
                barContainer.className = 'progress-bar-container';
                const bar = document.createElement('div');
                bar.className = 'progress-bar';
                bar.style.width = (item.completionRate || 0) + '%';
                barContainer.appendChild(bar);
                tdBar.appendChild(barContainer);
                tr.appendChild(tdBar);
                // 新增：已评分专家真实姓名
                const tdExperts = document.createElement('td');
                tdExperts.textContent = Array.isArray(item.scoredExpertNames) && item.scoredExpertNames.length > 0 ? item.scoredExpertNames.join(', ') : '-';
                tr.appendChild(tdExperts);
                // 总分（所有专家评分总和），仅在项目已评审时显示
                const tdScore = document.createElement('td');
                tdScore.className = 'score-info';
                if (item.isReviewed === 1 || item.is_reviewed === 1) {
                    tdScore.textContent = item.totalScore != null ? item.totalScore.toFixed(2) : '-';
                } else {
                    tdScore.textContent = '-';
                }
                tr.appendChild(tdScore);

                // 操作列：明细按钮
                const tdAction = document.createElement('td');
                const detailBtn = document.createElement('button');
                detailBtn.className = 'btn btn-detail';
                detailBtn.textContent = '明细';
                detailBtn.onclick = function() {
                    showScoreDetailModal(item.projectId, taskId, item.projectName);
                };
                tdAction.appendChild(detailBtn);
                tr.appendChild(tdAction);

                tbody.appendChild(tr);
            });

            // 只在任务未完成时显示切换模式区域
            if (!isCompleted) {
                showSwitchModeInfo(taskResponse.data, data);
            } else {
                $('#switchModeInfo').hide();
            }

            if (isAuto && !isCompleted) {
                countdown = 30;
                updateCountdownDisplay();
            }
            updateExportBtnVisibility(isCompleted);
        }
    }).catch(error => {
        showError('加载进度数据失败:', error);
    }).finally(() => {
        hideLoading();
    });
}

// 显示切换模式信息
function showSwitchModeInfo(task, projectList) {
    // 1. 任务为同步评审类型（taskType === 1为同步）
    const isSyncTask = task && task.taskType === 1;
    // 2. 任务切换模式为手动（switchMode === 2为手动）
    const isManualSwitch = task && task.switchMode === 2;
    // 3. 当前项目is_reviewed为0（未评审）
    let showManualBtn = false;
    let currentProject = null;
    
    if (Array.isArray(projectList)) {
        currentProject = projectList.find(p => p.isReviewed === 0 || p.is_reviewed === 0);
    }
    
    if (isSyncTask && isManualSwitch && currentProject && (currentProject.isReviewed === 0 || currentProject.is_reviewed === 0)) {
        showManualBtn = true;
    }
    
    if (isSyncTask) {
        $('#switchModeInfo').show();
        let btnText = isManualSwitch ? '切换为自动切换' : '切换为手动切换';
        let modeText = isManualSwitch ? '手动切换' : '自动切换';
        $('#switchModeBtn').text(btnText);
        $('#switchModeText').text('当前切换模式：' + modeText);
        
        if (showManualBtn) {
            $('#manualSwitchBtn').show();
            $('#manualSwitchBtn').prop('disabled', false);
        } else {
            $('#manualSwitchBtn').hide();
        }

        // 新增：检查是否显示"调整项目顺序"按钮
        if (isManualSwitch && task.status !== 'completed') {
            $('#reorderBtn').show();
        } else {
            $('#reorderBtn').hide();
        }
    } else {
        $('#switchModeInfo').hide();
        $('#manualSwitchBtn').hide();
        $('#reorderBtn').hide();
    }
}

// 检查项目顺序调整权限
function checkReorderPermission(taskId) {
    api.getReorderPermission(taskId)
        .then(response => {
            if (response.success) {
                const permission = response.data;
                if (permission.canReorder) {
                    showReorderControls();
                    initializeProjectReorder(taskId);
                } else {
                    hideReorderControls();
                    console.log('无法调整项目顺序:', permission.reason);
                }
            }
        })
        .catch(error => {
            console.error('检查权限失败:', error);
            hideReorderControls();
        });
}

// 显示项目顺序调整控件
function showReorderControls() {
    // 添加项目顺序调整区域
    if ($('#reorderSection').length === 0) {
        const reorderHtml = `
            <div id="reorderSection" class="reorder-section">
                <h3>项目顺序调整</h3>
                <p class="reorder-tip">拖拽项目可调整评审顺序（仅限未开始评审的项目）</p>
                <div id="projectReorderList" class="project-reorder-list"></div>
                <div class="reorder-actions">
                    <button id="saveReorderBtn" class="btn btn-primary">保存顺序</button>
                    <button id="cancelReorderBtn" class="btn btn-secondary">取消</button>
                </div>
            </div>
        `;
        $('#switchModeInfo').after(reorderHtml);
    }
    $('#reorderSection').show();
}

// 隐藏项目顺序调整控件
function hideReorderControls() {
    $('#reorderSection').hide();
}

// 初始化项目顺序调整功能
function initializeProjectReorder(taskId) {
    // 检查jQuery UI Sortable是否可用
    if (typeof $.fn.sortable === 'undefined') {
        console.error('jQuery UI Sortable插件未加载，请检查jQuery UI库是否正确引入');
        return;
    }
    
    // 获取当前任务的项目列表
    api.getTaskProjectProgressAndScores(taskId)
        .then(response => {
            if (response.success) {
                renderReorderProjectList(response.data, taskId);
            }
        })
        .catch(error => {
            console.error('获取项目列表失败:', error);
        });
}

// 渲染项目顺序调整列表
function renderReorderProjectList(projectList, taskId) {
    const $container = $('#projectReorderList');
    $container.empty();

    // 只显示尚未评分的项目（未评分且未正在评分）
    // 需同时满足 isReviewed/is_reviewed === 0 且 completedExperts === 0
    const unreviewedProjects = projectList.filter(project =>
        (project.isReviewed === 0 || project.is_reviewed === 0) &&
        (project.completedExperts === 0)
    );

    if (unreviewedProjects.length === 0) {
        $container.html('<p class="no-projects">所有项目已开始评审，无法调整顺序</p>');
        return;
    }

    unreviewedProjects.forEach((project, index) => {
        const $projectItem = $(`
            <div class="reorder-project-item" data-project-id="${project.projectId}">
                <span class="handle">☰</span>
                <span class="project-name">${project.projectName}</span>
                <span class="project-order">#${index + 1}</span>
            </div>
        `);
        $container.append($projectItem);
    });

    // 初始化拖拽排序
    $container.sortable({
        handle: '.handle',
        placeholder: 'ui-sortable-placeholder',
        tolerance: 'pointer',
        opacity: 0.8,
        update: function(event, ui) {
            updateReorderOrder();
        }
    }).disableSelection();

    // 绑定保存按钮事件
    $('#saveReorderBtn').off('click').on('click', function() {
        saveProjectOrder(taskId);
    });

    // 绑定取消按钮事件（修复：直接关闭弹窗）
    $('#cancelReorderBtn').off('click').on('click', function() {
        closeReorderModal();
    });
}

// 更新项目顺序显示
function updateReorderOrder() {
    $('#projectReorderList .reorder-project-item').each(function(index) {
        $(this).find('.project-order').text(`#${index + 1}`);
    });
}

// 显示项目顺序调整弹窗
function showReorderModal(taskId) {
    // 显示弹窗
    $('#reorderModal').show();
    
    // 初始化项目顺序调整功能
    initializeProjectReorder(taskId);
}

// 关闭项目顺序调整弹窗
function closeReorderModal() {
    // 隐藏弹窗
    $('#reorderModal').hide();
    
    // 重新启动自动刷新
    const taskId = $('#taskSelect').val();
    if (taskId) {
        startAutoRefresh(taskId);
    }
}

// 保存项目顺序
function saveProjectOrder(taskId) {
    const projectIds = $('#projectReorderList .reorder-project-item').map(function() {
        return $(this).data('project-id');
    }).get();

    if (projectIds.length === 0) {
        alert('没有可调整顺序的项目');
        return;
    }

    // 显示确认对话框
    if (!confirm(`确定要调整这 ${projectIds.length} 个项目的评审顺序吗？`)) {
        return;
    }

    // 显示加载状态
    $('#saveReorderBtn').prop('disabled', true).text('保存中...');

    api.reorderTaskProjects(taskId, projectIds)
        .then(response => {
            if (response.success) {
                alert('项目顺序调整成功');
                // 关闭弹窗
                closeReorderModal();
                // 重新加载进度数据
                loadProgress(taskId, false);
            } else {
                alert('调整失败: ' + (response.message || '未知错误'));
            }
        })
        .catch(error => {
            console.error('保存项目顺序失败:', error);
            alert('保存失败: ' + (error.message || '未知错误'));
        })
        .finally(() => {
            $('#saveReorderBtn').prop('disabled', false).text('保存顺序');
        });
}

// 明细弹窗逻辑
function showScoreDetailModal(projectId, taskId, projectName) {
    $('#scoreDetailModal').show();
    $('#scoreDetailContent').html('加载中...');
    api.getProjectScoreDetails(projectId, taskId)
        .then(response => {
            if (response.success && Array.isArray(response.data)) {
                // 只保留score不为null的记录
                const filtered = response.data.filter(row => row.score != null);
                if (filtered.length === 0) {
                    $('#scoreDetailContent').html('暂无评分明细');
                    return;
                }
                let html = `<h4 style=\"margin-bottom:12px;\">${projectName}</h4><table class=\"user-table score-detail-table\"><thead><tr><th>评分项</th><th>专家</th><th>分数</th></tr></thead><tbody>`;
                filtered.forEach(row => {
                    html += `<tr>
                        <td>${row.scoreItemName}</td>
                        <td>${row.expertName}</td>
                        <td>${row.score}</td>
                    </tr>`;
                });
                html += '</tbody></table>';
                $('#scoreDetailContent').html(html);
            } else {
                $('#scoreDetailContent').html('暂无评分明细');
            }
        })
        .catch(() => {
            $('#scoreDetailContent').html('加载失败');
        });
}
$('#closeScoreDetail').on('click', function() {
    $('#scoreDetailModal').hide();
});

// 导出按钮逻辑
function updateExportBtnVisibility(isCompleted) {
    if (isCompleted) {
        $('#exportBtn').show();
    } else {
        $('#exportBtn').hide();
    }
}

// 导出按钮逻辑
$('#exportBtn').on('click', function() {
    const taskId = $('#taskSelect').val();
    if (!taskId) {
        showError('请先选择评审任务');
        return;
    }
    api.exportTaskExcel(taskId)
        .then(blob => {
            // 创建下载链接
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `task_${taskId}_scores.xlsx`;
            document.body.appendChild(a);
            a.click();
            a.remove();
            window.URL.revokeObjectURL(url);
        })
        .catch(() => {
            showError('导出失败');
        });
});

// 刷新按钮点击事件
$('#refreshBtn').on('click', function() {
    const taskId = $('#taskSelect').val();
    if (taskId) {
        loadProgress(taskId, false);
        countdown = 30; // 重置倒计时
        updateCountdownDisplay();
    }
});

function showLoading() {
    $('.loading-overlay').show();
}
function hideLoading() {
    $('.loading-overlay').hide();
}

function showError(message) {
    // 参考 scoring.js 的实现，插入到主内容区下方
    /**let $error = $('.error-message');
    if ($error.length === 0) {
        $error = $('<div class="error-message"></div>').insertAfter('.main-content');
    }
    $error.text(message).stop(true, true).fadeIn().delay(2000).fadeOut();**/
    alert(message);
}

// 3. 任务切换事件
$(document).ready(function() {
    // 检查元素是否存在
    console.log('taskSelect元素是否存在:', $('#taskSelect').length);

    // 绑定事件
    $('#taskSelect').on('change', function() {
        const taskId = $(this).val();
        if (timer) clearInterval(timer); // 先停用旧定时器
        if (taskId) {
            loadProgress(taskId, false);
            startAutoRefresh(taskId);
        }
    });

    // 切换模式按钮事件
    $('#switchModeBtn').on('click', function() {
        const taskId = $('#taskSelect').val();
        if (!taskId) {
            showError('请先选择评审任务');
            return;
        }

        const newMode = $(this).text().includes('自动') ? 1 : 2; // 1:自动, 2:手动
        api.updateTaskSwitchMode(taskId, newMode)
            .then(response => {
                if (response.success) {
                    loadProgress(taskId, false);
                } else {
                    showError('切换模式失败：' + (response.message || '未知错误'));
                }
            })
            .catch(error => {
                showError('切换模式失败：' + error.message);
            });
    });

    // 手动切换按钮事件
    $('#manualSwitchBtn').on('click', function() {
    const taskId = $('#taskSelect').val();
        if (!taskId) {
            showError('请先选择评审任务');
            return;
        }

        // 获取当前未评审项目（与高亮显示一致）
        const projectList = window.currentProjectList || [];
        const currentProject = projectList.find(p => p.isReviewed === 0 || p.is_reviewed === 0);
        if (!currentProject) {
            showError('未找到可切换的项目');
            return;
        }

        $(this).prop('disabled', true);
        api.manualSwitch(taskId, currentProject.projectId)
            .then(response => {
                if (response.success) {
                    showError('切换成功，正在刷新数据...');
                    loadProgress(taskId, false);
                } else {
                    showError('切换失败：' + (response.message || '未知错误'));
                }
            })
            .catch(error => {
                showError('切换失败：' + error.message);
            })
            .finally(() => {
                $(this).prop('disabled', false);
            });
    });

    // 调整项目顺序按钮事件
    $('#reorderBtn').on('click', function() {
        const taskId = $('#taskSelect').val();
        if (!taskId) {
            showError('请先选择评审任务');
            return;
        }

        // 停止自动刷新
        if (timer) {
            clearInterval(timer);
            timer = null;
        }

        // 显示项目顺序调整弹窗
        showReorderModal(taskId);
    });

    // 关闭项目顺序调整弹窗事件
    $('#closeReorderModal').on('click', function() {
        closeReorderModal();
    });

    // 取消项目顺序调整事件
    $('#cancelReorderBtn').on('click', function() {
        closeReorderModal();
    });

    // 确认事件绑定
    console.log('已绑定change事件到taskSelect');
});