$(document).ready(function() {
    // 验证用户权限 - 使用新的角色检查函数
    if (!auth.isLoggedIn() || !auth.isAdmin()) {
        console.log('权限验证失败 - 用户角色:', auth.getRole());
        window.location.href = '../index.html';
        return;
    }

    // 显示用户信息和初始化退出功能
    initializeUserInfo();
    
    // 加载项目列表
    loadProjects();

    // 新增：加载任务列表
    loadTasks();
    
    // 修改新建项目按钮事件
    $('#addProjectBtn').on('click', async function(e) { // Make handler async
        e.preventDefault();
    try {
        // 确保其他对话框都已关闭
        closeAllDialogs();
        
        // 获取专家角色列表
        const availableRoles = await getAvailableRoles();
        if (!Array.isArray(availableRoles) || availableRoles.length === 0) {
            throw new Error('没有可用的评审专家');
        }

        // 打开项目对话框并加载角色
        await openDialog('新建项目', null, availableRoles);
        // 打开模板选择对话框
        openTemplateDialog();
    } catch (error) {
        console.error('初始化新建项目对话框失败:', error);
        alert('初始化新建项目对话框失败: ' + error.message);
    }
    });
    
    // 绑定项目表单提交事件
    $('#projectForm').on('submit', handleProjectSubmit);
    
    // 绑定添加评分项按钮事件 - 修正为按组别添加
    $(document).on('click', '.btn-add-score-item', async function() {
        try {
            const group = $(this).data('group');
            if (!group) {
                throw new Error('无法确定评分组');
            }

        // 获取已有的角色列表（从第一个评分项的下拉框中）
        let availableRoles = [];
            const $firstSelect = $(`#${group}ScoreItems .role-select`).first();
        if ($firstSelect.length > 0) {
            availableRoles = $firstSelect.find('option').map(function() {
                return $(this).val();
            }).get();
        } else {
            // 如果还没有评分项，重新获取角色列表
            availableRoles = await getAvailableRoles();
            if (!Array.isArray(availableRoles) || availableRoles.length === 0) {
            throw new Error('无法获取评审专家列表');
            }
        }

            addScoreItemRow(group, '', [], 0, 100, availableRoles);
    } catch (error) {
        console.error('添加评分项失败:', error);
        alert('添加评分项失败: ' + error.message);
    } 
    });
    
    // 绑定删除评分项按钮事件
    $(document).on('click', '.btn-remove', function() {
        $(this).closest('.score-item').remove();
    });
    
    // 初始化批量操作功能
    initBatchOperations();
    
    // 初始化高级搜索
    initializeAdvancedSearch();

    // 初始化任务管理
    initTaskManagement();

    // 绑定对话框关闭按钮事件
    $('.dialog-close').off('click').on('click', function() {
        const $dialog = $(this).closest('.dialog');
        if ($dialog.is('#projectDialog')) {
            closeDialog();
        } else if ($dialog.is('#templateDialog')) {
            closeTemplateDialog();
        } else if ($dialog.is('#taskDialog')) {
            closeTaskDialog();
        }
    });

    // 绑定对话框取消按钮事件
    $('.btn-cancel').off('click').on('click', function() {
        const $dialog = $(this).closest('.dialog');
        if ($dialog.is('#projectDialog')) {
            closeDialog();
        } else if ($dialog.is('#templateDialog')) {
            closeTemplateDialog();
        } else if ($dialog.is('#taskDialog')) {
            closeTaskDialog();
        }
    });

    // 初始化任务管理
    initTaskManagement();
});

function initializeUserInfo() {
    const userInfo = auth.getUserInfo();
    $('#currentUser').text(userInfo.username);
    $('#logoutBtn').on('click', function(e) {
        e.preventDefault();
        if (confirm('是否清空本地缓存数据？\n选择"确定"将清空所有缓存，避免影响下一次测试。\n选择"取消"将保留缓存数据。')) {
            // 清空所有相关缓存数据
            localStorage.clear();
            console.log('已清空所有缓存数据');
        } else {
            // 只清除登录信息
        auth.removeUserInfo();
            console.log('仅退出登录，未清空其他缓存');
        }
        window.location.href = '../index.html';
    });
}

function loadProjects() {
    api.getSimpleProjects()
        .then(response => {
            if (response.success) {
                window.allProjects = response.data; // 缓存所有项目
                renderProjects(response.data);
            }
        })
        .catch(error => {
            alert('加载项目列表失败');
            console.error(error);
        });
}

function renderProjects(projects) {
    const $projectList = $('#projectList');
    $projectList.empty();
    
    projects.forEach(project => {
        const $card = $(`
            <div class="project-card ${project.status === 'archived' ? 'archived' : ''}" data-id="${project.id}" data-status="${project.status}" data-create-time="${project.createTime}">
                <div class="project-select">
                    <input type="checkbox" class="project-checkbox">
                </div>
                <h3>${project.name}</h3>
                <div class="project-info">
                    <div class="info-item">
                        <label>单位：</label>
                        <span>${project.unit || '未填写'}</span>
                    </div>
                    <div class="info-item">
                        <label>团队代表：</label>
                        <span>${project.leader || '未填写'}</span>
                    </div>
                    <div class="info-item">
                        <label>创建时间：</label>
                        <span>${new Date(project.createTime).toLocaleString()}</span>
                    </div>
                    <div class="info-item">
                        <label>状态：</label>
                        <span>${getStatusText(project.status)}</span>
                    </div>
                </div>
                <div class="project-actions">
                    <button class="btn btn-edit">编辑</button>
                    ${project.status !== 'archived' ? 
                        `<button class="btn btn-archive">归档</button>` : 
                        `<button class="btn btn-restore">还原</button>`
                    }
                    <button class="btn btn-delete">删除</button>
                </div>
            </div>
        `);
        
        // 绑定按钮事件
        $card.find('.btn-edit').on('click', async () => await editProject(project));
        $card.find('.btn-archive').on('click', () => archiveProject(project.id));
        $card.find('.btn-restore').on('click', () => restoreProject(project.id));
        $card.find('.btn-delete').on('click', () => deleteProject(project.id));
        
        $projectList.append($card);
    });
}

function getStatusText(status) {
    const statusMap = {
        'draft': '草稿',
        'active': '已启用',
        'inactive': '已禁用',
        'pending': '待启用',  // 添加 pending 状态的翻译
        'archived': '已归档'  // 添加 archived 状态的翻译，以保持完整性
    };
    return statusMap[status] || status;
}

// 修改后的 openDialog 函数，获取并传递角色列表
async function openDialog(title, project = null, existingRoles = null) {
    $('#dialogTitle').text(title);
    const $form = $('#projectForm');
    $form[0].reset();

    try {
        // 获取角色列表
        let availableRoles = existingRoles;
        if (!availableRoles) {
            availableRoles = await getAvailableRoles();
        }
        
        if (!Array.isArray(availableRoles) || availableRoles.length === 0) {
            throw new Error('没有可用的评审专家');
        }

        if (project) {
            // 编辑项目时填充表单
            $form.data('projectId', project.id);
            $('#projectName').val(project.name);
            $('#projectUnit').val(project.unit);
            $('#projectLeader').val(project.leader);

            // 保存当前项目数据，用于提交时保持状态
            window.currentProjectData = project;

            // 填充各组评分项
            ['preliminary', 'semifinal', 'final'].forEach(group => {
                $(`#${group}ScoreItems`).empty();
                if (project.scoreGroups && project.scoreGroups[group]) {
                    project.scoreGroups[group].forEach(item => {
                        addScoreItemRow(group, item.name, item.roles, item.minScore, item.maxScore, availableRoles);
                    });
                }
            });
        } else {
            // 新建项目时清空表单
            $form.removeData('projectId');
            window.currentProjectData = null;
            ['preliminary', 'semifinal', 'final'].forEach(group => {
                $(`#${group}ScoreItems`).empty();
            });
        }

        // 初始化评分项按钮
        initializeScoreItemButtons();

        $('#projectDialog').fadeIn(200);
    } catch (error) {
        console.error('初始化项目对话框失败:', error);
        alert('初始化项目对话框失败: ' + error.message);
    }
}

// 修改项目提交处理函数
function handleProjectSubmit(e) {
    e.preventDefault();

    const projectId = $('#projectForm').data('projectId');
    
    const projectData = {
        name: $('#projectName').val(),
        unit: $('#projectUnit').val(),
        leader: $('#projectLeader').val(),
        scoreGroups: {
            preliminary: [],
            semifinal: [],
            final: []
        }
    };

    // 如果是编辑项目，保持原有状态；如果是新建项目，设置默认状态
    if (projectId) {
        // 编辑项目时，从当前项目数据中获取状态和显示顺序
        const currentProject = window.currentProjectData;
        if (currentProject) {
            if (currentProject.status) {
                projectData.status = currentProject.status;
            }
            if (currentProject.displayOrder !== undefined && currentProject.displayOrder !== null) {
                projectData.displayOrder = currentProject.displayOrder;
            }
        }
    } else {
        // 新建项目时，设置默认状态
        projectData.status = 'draft';
    }

    // 收集各组评分项数据
    ['preliminary', 'semifinal', 'final'].forEach(group => {
        $(`#${group}ScoreItems .score-item`).each(function() {
            const $item = $(this);
            const name = $item.find('input[name="scoreItems[]"]').val();
            const minScore = parseInt($item.find('input[name="minScore[]"]').val());
            const maxScore = parseInt($item.find('input[name="maxScore[]"]').val());
            const roles = $item.find('select[name="scoreItemRoles[]"]').val() || [];

            if (name && roles.length > 0) {
                projectData.scoreGroups[group].push({
                    name,
                    minScore,
                    maxScore,
                    roles
                });
            }
        });
    });

    // 验证至少有一组评分项
    const hasScoreItems = Object.values(projectData.scoreGroups)
        .some(group => group.length > 0);
    if (!hasScoreItems) {
        alert('请至少添加一组评分项');
        return;
    }

    const apiCall = projectId
        ? api.updateProject(projectId, projectData)
        : api.createProject(projectData);

    apiCall.then(response => {
        if (response.success) {
            closeDialog();
            loadProjects();
            alert(projectId ? '项目更新成功' : '项目创建成功');
        }
    }).catch(error => {
        alert(error.message || '保存失败');
    });
}

// 修改后的 addScoreItemRow 函数，支持角色关联
// function addScoreItemRow(name = '', weight = '', roles = []) { // Original function
// ... original code ...
// }

// 修改添加评分项函数，添加组别参数
function addScoreItemRow(group, name = '', roles = [], minScore = 0, maxScore = 100, availableRoles = []) {
    const $container = $(`#${group}ScoreItems`);
    if (!Array.isArray(availableRoles) || availableRoles.length === 0) {
        console.error('No available roles provided');
        return;
    }
    
    // 确保 roles 是数组
    if (!Array.isArray(roles)) {
        roles = roles ? [roles] : [];
    }

    // 确保 availableRoles 是数组
    if (!Array.isArray(availableRoles)) {
        availableRoles = availableRoles ? [availableRoles] : [];
    }

    const $scoreItem = $(`
        <div class="score-item">
            <input type="text" name="scoreItems[]" placeholder="评分项名称" value="${name || ''}">
            <div class="score-range">
                <span>分值区间：</span>
                <input type="number" name="minScore[]" value="${minScore}" min="0" max="100">
                <span>-</span>
                <input type="number" name="maxScore[]" value="${maxScore}" min="0" max="100">
            </div>
            <select name="scoreItemRoles[]" multiple class="role-select">
                ${availableRoles.map(role => {
                    const roleStr = String(role);
                    const displayName = roleStr.replace('expert', '');
                    return `
                        <option value="${roleStr}" ${roles.includes(roleStr) ? 'selected' : ''}>
                            评审专家${displayName}
                    </option>
                    `;
                }).join('')}
            </select>
            <button type="button" class="btn-remove">删除</button>
        </div>
    `);

    // 添加分值区间验证
    const $minScore = $scoreItem.find('input[name="minScore[]"]');
    const $maxScore = $scoreItem.find('input[name="maxScore[]"]');

    $minScore.on('change', function() {
        const min = parseInt($(this).val());
        const max = parseInt($maxScore.val());
        if (min >= max) {
            alert('最小值必须小于最大值');
            $(this).val(0);
        }
    });

    $maxScore.on('change', function() {
        const min = parseInt($minScore.val());
        const max = parseInt($(this).val());
        if (max <= min) {
            alert('最大值必须大于最小值');
            $(this).val(100);
        }
    });

    $container.append($scoreItem);
}

// 修改后的 editProject 函数，使其成为异步
async function editProject(project) {
    // project 只需包含 id
    const response = await api.getProject(project.id);
    if (response.success) {
        await openDialog('编辑项目', response.data);
    } else {
        alert('获取项目信息失败');
    }
}

function toggleProjectStatus(project) {
    const newStatus = project.status === 'active' ? 'inactive' : 'active';
    api.updateProject(project.id, { status: newStatus })
        .then(response => {
            if (response.success) {
                loadProjects();
            }
        })
        .catch(error => {
            alert(error.message || '操作失败');
        });
}

function deleteProject(projectId) {
    if (confirm('确定要删除此项目吗？')) {
        api.deleteProject(projectId)
            .then(response => {
                if (response.success) {
                    loadProjects();
                }
            })
            .catch(error => {
                alert(error.message || '删除失败');
            });
    }
}

// 项目模板数据 (示例，实际数据可能来自 mock API 或其他地方)
const projectTemplates = {
    preliminary: {
        name: '初赛评审模板',
        description: '包含产业、投融资、知识产权三个评分项',
        scoreItems: [
            { 
                name: '产业',
                roles: ['expert1']
            },
            {
                name: '投融资',
                roles: ['expert2']
            },
            {
                name: '知识产权',
                roles: ['expert3']
            }
        ]
    },
    semifinal: {
        name: '复赛评审模板',
        description: '包含产业技术、投融资、知识产权、企业高管四个评分项',
        scoreItems: [
            { 
                name: '产业技术',
                roles: ['expert1']
            },
            {
                name: '投融资',
                roles: ['expert2']
            },
            {
                name: '知识产权',
                roles: ['expert3']
            },
            {
                name: '企业高管',
                roles: ['expert4']
            }
        ]
    },
    final: {
        name: '决赛评审模板',
        description: '包含投资、知识产权、产业技术、技术经理人、企业高管五个评分项',
        scoreItems: [
            { 
                name: '投资',
                roles: ['expert1']
            },
            {
                name: '知识产权',
                roles: ['expert2']
            },
            {
                name: '产业技术',
                roles: ['expert3']
            },
            {
                name: '技术经理人',
                roles: ['expert5']
            },
            {
                name: '企业高管',
                roles: ['expert4']
            }
        ]
    },
    fullProcess: {
        name: '高转赛模板',
        description: '包含初赛、复赛、决赛三个阶段的完整评分项组合',
        scoreGroups: {
            preliminary: [
                { 
                    name: '产业',
                    roles: ['expert1', 'expert2']
                },
                {
                    name: '投融资',
                    roles: ['expert2', 'expert3']
                },
                {
                    name: '知识产权',
                    roles: ['expert1', 'expert3']
                }
            ],
            semifinal: [
                { 
                    name: '产业技术',
                    roles: ['expert1', 'expert2']
                },
                {
                    name: '投融资',
                    roles: ['expert2', 'expert3']
                },
                {
                    name: '知识产权',
                    roles: ['expert1', 'expert3']
                },
                {
                    name: '企业高管',
                    roles: ['expert1', 'expert2', 'expert4']
                }
            ],
            final: [
                { 
                    name: '投资',
                    roles: ['expert1']
                },
                {
                    name: '知识产权',
                    roles: ['expert2']
                },
                {
                    name: '产业技术',
                    roles: ['expert3']
                },
                {
                    name: '技术经理人',
                    roles: ['expert1', 'expert2']
                },
                {
                    name: '企业高管',
                    roles: ['expert2', 'expert3', 'expert4']
                }
            ]
        }
    }
};

// 打开项目模板选择对话框
function openTemplateDialog() {
     closeAllDialogs();
    $('#templateDialog').fadeIn(200);
}

// 关闭项目模板选择对话框
function closeTemplateDialog() {
    $('#templateDialog').fadeOut(200);
}

// 修改 useTemplate 函数，支持处理完整评分组模板
async function useTemplate() {
    const $selected = $('.template-item.selected');
    if ($selected.length === 0) {
        alert('请选择一个项目模板');
        return;
    }

    const templateKey = $selected.data('template');
    const template = projectTemplates[templateKey];

    try {
        // 获取专家角色列表
        const availableRoles = await getAvailableRoles();
        if (!Array.isArray(availableRoles) || availableRoles.length === 0) {
            throw new Error('没有可用的评审专家');
        }

        // 填充表单
        $('#projectName').val(template.name || '');
        $('#projectUnit').val(template.unit || '');
        $('#projectLeader').val(template.leader || '');

        // 清空所有评分项组
        ['preliminary', 'semifinal', 'final'].forEach(group => {
            $(`#${group}ScoreItems`).empty();
        });

        if (template.scoreGroups) {
            // 处理完整评分组模板
            ['preliminary', 'semifinal', 'final'].forEach(group => {
                const groupItems = template.scoreGroups[group];
                if (Array.isArray(groupItems)) {
                    groupItems.forEach(item => {
                        addScoreItemRow(
                            group,
                            item.name,
                            item.roles || [],
                            item.minScore || 0,
                            item.maxScore || 100,
                            availableRoles
                        );
                    });
                }
            });
        } else if (template.scoreItems) {
            // 兼容处理单组评分项模板
            template.scoreItems.forEach(item => {
                addScoreItemRow(
                    'preliminary',
                    item.name,
                    item.roles || [],
                    item.minScore || 0,
                    item.maxScore || 100,
                    availableRoles
                );
            });
        }

        closeTemplateDialog();
        $('#projectDialog').fadeIn(200);
    } catch (error) {
        console.error('应用模板失败:', error);
        alert('应用模板失败: ' + error.message);
    }
}

// 项目归档功能
function archiveProject(projectId) {
    if (confirm('确定要归档此项目吗？')) {
        api.updateProject(projectId, { status: 'archived' })
            .then(response => {
                if (response.success) {
                    loadProjects();
                }
            })
            .catch(error => {
                alert(error.message || '归档失败');
            });
    }
}

// 批量归档项目
function batchArchiveProjects(projectIds) {
    api.batchUpdateProjects(projectIds, 'archived')
        .then(response => {
            if (response.success) {
                loadProjects();
                $('.btn-batch-cancel').click();
            }
        })
        .catch(error => {
            alert(error.message || '批量归档失败');
        });
}

// 初始化批量操作功能
function initBatchOperations() {
    // 绑定批量操作按钮事件
    $('#batchActionBtn').on('click', function() {
        const $toolbar = $('.batch-toolbar');
        if ($toolbar.is(':visible')) {
            exitBatchMode();
        } else {
            enterBatchMode();
        }
    });

    // 绑定批量操作按钮事件
    $('.btn-batch-archive').on('click', handleBatchArchive);
    $('.btn-batch-delete').on('click', handleBatchDelete);
    $('.btn-batch-cancel').on('click', exitBatchMode);

    // 绑定全选checkbox事件
    $('#selectAll').on('change', function() {
        $('.project-checkbox').prop('checked', this.checked);
        updateSelectedCount();
    });

    // 绑定单个项目checkbox事件
    $(document).on('change', '.project-checkbox', updateSelectedCount);
}

function enterBatchMode() {
    $('.batch-toolbar').slideDown(200);
    $('.project-select').show();
    $('#batchActionBtn').addClass('active');
    updateSelectedCount();
}

function exitBatchMode() {
    $('.batch-toolbar').slideUp(200);
    $('.project-select').hide();
    $('#batchActionBtn').removeClass('active');
    $('.project-checkbox').prop('checked', false);
    updateSelectedCount();
}

function updateSelectedCount() {
    const count = $('.project-checkbox:checked').length;
    $('#selectedCount').text(count);
    
    // 更新批量操作按钮状态
    $('.btn-batch-archive, .btn-batch-delete').prop('disabled', count === 0);
}

function getSelectedProjectIds() {
    return $('.project-checkbox:checked').map(function() {
        return $(this).closest('.project-card').data('id');
    }).get();
}

function handleBatchArchive() {
    const selectedIds = getSelectedProjectIds();
    if (selectedIds.length === 0) {
        alert('请选择要归档的项目');
        return;
    }

    if (confirm(`确定要归档选中的 ${selectedIds.length} 个项目吗？`)) {
        batchArchiveProjects(selectedIds);
    }
}

function handleBatchDelete() {
    const selectedIds = getSelectedProjectIds();
    if (selectedIds.length === 0) {
        alert('请选择要删除的项目');
        return;
    }

    if (confirm(`确定要删除选中的 ${selectedIds.length} 个项目吗？此操作不可恢复！`)) {
        api.batchDeleteProjects(selectedIds)
            .then(response => {
                if (response.success) {
                    loadProjects();
                    exitBatchMode();
                }
            })
            .catch(error => {
                alert(error.message || '批量删除失败');
            });
    }
}

// 绑定模板选择事件
$('.template-item').on('click', function() {
    $('.template-item').removeClass('selected');
    $(this).addClass('selected');
});

function initializeAdvancedSearch() {
    const $searchBar = $(`
        <div class="search-bar">
            <input type="text" id="searchInput" placeholder="搜索项目...">
            <select id="statusFilter">
                <option value="">全部状态</option>
                <option value="draft">草稿</option>
                <option value="active">已启用</option>
                <option value="inactive">已禁用</option>
                <option value="archived">已归档</option>
            </select>
            <select id="sortBy">
                <option value="createTime">创建时间</option>
                <option value="name">项目名称</option>
                <option value="status">项目状态</option>
            </select>
            <select id="sortOrder">
                <option value="desc">降序</option>
                <option value="asc">升序</option>
            </select>
        </div>
    `);

    $('.header').after($searchBar);

    // 绑定搜索和筛选事件
    $('#searchInput, #statusFilter, #sortBy, #sortOrder').on('change input', function() {
        applyFiltersAndSort();
    });
}

function applyFiltersAndSort() {
    const searchText = $('#searchInput').val().toLowerCase();
    const statusFilter = $('#statusFilter').val();
    const sortBy = $('#sortBy').val();
    const sortOrder = $('#sortOrder').val();

    // 获取所有项目卡片
    const $cards = $('.project-card').toArray();

    // 筛选
    const filteredCards = $cards.filter(card => {
        const $card = $(card);
        const name = $card.find('h3').text().toLowerCase();
        const status = $card.data('status');

        const matchesSearch = !searchText || name.includes(searchText);
        const matchesStatus = !statusFilter || status === statusFilter;

        return matchesSearch && matchesStatus;
    });

    // 排序
    filteredCards.sort((a, b) => {
        const $a = $(a);
        const $b = $(b);
        let comparison = 0;

        switch (sortBy) {
            case 'name':
                comparison = $a.find('h3').text().localeCompare($b.find('h3').text());
                break;
            case 'createTime':
                comparison = new Date($a.data('createTime')) - new Date($b.data('createTime'));
                break;
            case 'status':
                comparison = $a.data('status').localeCompare($b.data('status'));
                break;
        }

        return sortOrder === 'asc' ? comparison : -comparison;
    });

    // 更新显示
    const $projectList = $('#projectList');
    $projectList.empty().append(filteredCards);
}

// 修改初始化任务管理函数，确保事件只绑定一次
function initTaskManagement() {
    // 添加创建任务按钮
    $('.header-actions').find('#createTaskBtn').remove(); // 确保不重复添加按钮
    $('.header-actions').prepend(`
        <button id="createTaskBtn" class="btn">创建评审任务</button>
    `);

    // 绑定创建任务按钮事件
    $('#createTaskBtn').off('click').on('click', function(e) {
        e.preventDefault();
        openTaskDialog();
    });

    // 绑定任务表单提交事件
    $('#taskForm').off('submit').on('submit', handleTaskSubmit);

    // 解绑所有相关的事件处理器
    $(document).off('click', '.btn-enable-task');
    $(document).off('click', '.btn-edit-task');
    
    // 使用事件委托绑定任务操作事件
    $(document).on('click', '.btn-edit-task', function() {
        const taskId = $(this).data('taskId');
        // 从已加载的任务列表中查找任务
        const task = window.currentTasks ? window.currentTasks.find(t => t.id === taskId) : null;
        if (task) {
            openTaskDialog(task);
        }
    });
    
    // 使用事件委托绑定启用任务事件
    $(document).on('click', '.btn-enable-task', function() {
        const taskId = $(this).data('taskId');
        if (!taskId) {
            console.error('Failed to get task ID');
            return;
        }

        if (confirm(`确定要启用评审任务 [${taskId}] 吗？`)) {
            api.getReviewTask(taskId)
                .then(response => {
                    if (response.success && response.data) {
                        const task = response.data;
                        if (!task.expertIds || task.expertIds.length === 0) {
                            alert('请先指定评审专家后再启用任务');
                            return null;
                        }
                        return api.enableReviewTask(taskId);
                    }
                    throw new Error('获取任务信息失败');
                })
                .then(response => {
                    if (response && response.success) {
                        loadTasks();
                        alert('任务已启用');
                    }
                })
                .catch(error => {
                    if (error) {
                        alert('启用任务失败：' + (error.message || '未知错误'));
                    }
                });
        }
    });
}

// 新增: 支持只读模式
async function openTaskDialog(task = null, readOnly = false) {
    const $dialog = $('#taskDialog');
    const $form = $('#taskForm');
    const $title = $('#taskDialogTitle');

    // 确保其他对话框都已关闭
    closeDialog();
    closeTemplateDialog();

    // 更新标题和模式
    if (readOnly) {
        $title.text('任务详情');
        $form.addClass('readonly-mode');
        // 禁用所有表单控件
        $form.find('input, select, button').prop('disabled', true);
        // 使拖拽功能失效
        if ($('#selectedProjectsList').data('sortable')) {
            $('#selectedProjectsList').sortable('disable');
        }
        // 确保"取消"按钮可用，并将其文本改为"关闭"
        $form.find('.btn-cancel').prop('disabled', false).text('关闭');
        // 隐藏"保存"按钮
        $form.find('.btn-save').hide();
    } else {
        $title.text(task ? '编辑任务' : '创建评审任务');
        $form.removeClass('readonly-mode');
        // 启用所有表单控件
        $form.find('input, select, button').prop('disabled', false);
        // 启用拖拽
        if ($('#selectedProjectsList').data('sortable')) {
            $('#selectedProjectsList').sortable('enable');
        }
        $form.find('.btn-cancel').text('取消');
        $form.find('.btn-save').show().text('保存');
    }

    try {
        // 获取项目列表和专家列表
        const [projectsResponse, expertsResponse] = await Promise.all([
            api.getSimpleProjects(),  // 使用简化项目数据
            api.getUsers()
        ]);

        if (projectsResponse.success && expertsResponse.success) {
            // 处理已选择的项目ID列表
            let selectedProjectIds = [];
            if (task && task.projects && Array.isArray(task.projects)) {
                selectedProjectIds = task.projects.map(project => project.id);
            }
            
            // 处理已选择的专家ID列表
            let selectedExpertUsernames = [];
            if (task && task.experts && Array.isArray(task.experts)) {
                // 直接使用专家用户名列表
                selectedExpertUsernames = task.experts;
            }
            
            // 渲染项目选择
            renderTaskProjects(projectsResponse.data, selectedProjectIds);
            
            // 渲染专家选择
            renderTaskExperts(expertsResponse.data, selectedExpertUsernames);
            
            if (task) {
                // 填充任务数据
                $('#taskCategory').val(task.category || '');
                $(`input[name="taskType"][value="${task.taskType}"]`).prop('checked', true);
                $(`input[name="scoreGroupType"][value="${task.scoreGroupType}"]`).prop('checked', true);
                $('#taskForm').data('taskId', task.id);
            } else {
                // 新建任务时，彻底清空所有输入框和数据
                $('#taskForm')[0].reset();
                $('#taskForm').removeData('taskId');
                $('#taskCategory').val('');
                $('input[name="taskType"][value="1"]').prop('checked', true);
                $('input[name="scoreGroupType"][value="preliminary"]').prop('checked', true);
            }
            
            $('#taskDialog').fadeIn(200);
        }
    } catch (error) {
        console.error('初始化任务对话框失败:', error);
        alert('初始化任务对话框失败: ' + error.message);
    }
}

// 修改 renderTaskProjects 函数
function renderTaskProjects(projects, selectedIds = []) {
    const $container = $('#taskProjects');
    $container.empty();

    // 添加分组标题和容器
    $container.html(`
        <div class="task-projects-group">
            <div class="task-projects-group-title">已选择项目</div>
            <div id="selectedProjects" class="task-projects-sortable"></div>
        </div>
        <div class="task-projects-group">
            <div class="task-projects-group-title">未选择项目</div>
            <div id="unselectedProjects" class="task-projects-sortable"></div>
        </div>
    `);

    // 严格按selectedIds顺序渲染已选择项目
    const $selectedContainer = $('#selectedProjects');
    selectedIds.forEach((id, idx) => {
        const project = projects.find(p => p.id === id);
        if (project) {
            $selectedContainer.append(createProjectItemElement(project, true, idx));
        }
    });

    // 渲染未选择的项目
    const $unselectedContainer = $('#unselectedProjects');
    projects.forEach(project => {
        if (!selectedIds.includes(project.id)) {
            $unselectedContainer.append(createProjectItemElement(project, false));
        }
    });

    // 初始化拖拽排序
    $('#selectedProjects, #unselectedProjects').sortable({
        connectWith: '.task-projects-sortable',
        handle: '.handle',
        placeholder: 'ui-sortable-placeholder',
        tolerance: 'pointer',
        opacity: 0.8,
        update: function(event, ui) {
            if (this === ui.item.parent()[0]) {
                const $item = ui.item;
                const isSelected = $item.closest('#selectedProjects').length > 0;
                $item.toggleClass('selected', isSelected)
                     .toggleClass('unselected', !isSelected);
                updateProjectOrder();
            }
        }
    }).disableSelection();

    // 为未选择的项目添加双击事件
    $('#unselectedProjects .task-project-item').on('dblclick', function() {
        const $item = $(this);
        $item.appendTo('#selectedProjects')
             .removeClass('unselected')
             .addClass('selected');
        updateProjectOrder();
    });

    // 为已选择的项目添加双击事件（移回未选择）
    $('#selectedProjects .task-project-item').on('dblclick', function() {
        const $item = $(this);
        $item.appendTo('#unselectedProjects')
             .removeClass('selected')
             .addClass('unselected');
        updateProjectOrder();
    });
}

// 添加专家渲染函数
function renderTaskExperts(experts, selectedUsernames = []) {
    const $selectedContainer = $('#selectedExperts');
    const $unselectedContainer = $('#unselectedExperts');
    
    $selectedContainer.empty();
    $unselectedContainer.empty();

    // 只保留专家角色
    const expertList = experts.filter(expert => 
        expert.role && expert.role.toLowerCase().startsWith('expert')
    );

    expertList.forEach(expert => {
        const isSelected = selectedUsernames.includes(expert.username);
        const $expertItem = createExpertItemElement(expert, isSelected);
        if (isSelected) {
            $selectedContainer.append($expertItem);
        } else {
            $unselectedContainer.append($expertItem);
        }
    });

    // 初始化专家拖拽排序
    $('.task-experts-sortable').sortable({
        connectWith: '.task-experts-sortable',
        handle: '.handle',
        placeholder: 'ui-sortable-placeholder',
        tolerance: 'pointer',
        opacity: 0.8,
        update: function(event, ui) {
            if (this === ui.item.parent()[0]) {
                const $item = ui.item;
                const isSelected = $item.closest('#selectedExperts').length > 0;
                $item.toggleClass('selected', isSelected)
                     .toggleClass('unselected', !isSelected);
            }
        }
    }).disableSelection();

    // 为未选择的专家添加双击事件
    $('#unselectedExperts .expert-item').on('dblclick', function() {
        const $item = $(this);
        $item.appendTo('#selectedExperts')
             .removeClass('unselected')
             .addClass('selected');
    });

    // 为已选择的专家添加双击事件（移回未选择）
    $('#selectedExperts .expert-item').on('dblclick', function() {
        const $item = $(this);
        $item.appendTo('#unselectedExperts')
             .removeClass('selected')
             .addClass('unselected');
    });
}

// 添加创建项目元素的辅助函数
function createProjectItemElement(project, isSelected, index = null) {
    return $(`
        <div class="task-project-item ${isSelected ? 'selected' : 'unselected'}" data-id="${project.id}">
            <span class="handle">☰</span>
            <span class="project-name">${project.name}</span>
            <span class="order">${isSelected ? `#${index + 1}` : '--'}</span>
        </div>
    `);
}

// 添加创建专家元素的辅助函数
function createExpertItemElement(expert, isSelected) {
    return $(`
        <div class="expert-item ${isSelected ? 'selected' : 'unselected'}" data-id="${expert.username}">
            <span class="handle">☰</span>
            <span class="expert-name">${expert.name || expert.username}</span>
        </div>
    `);
}

// 更新 updateProjectOrder 函数，确保正确更新序号
function updateProjectOrder() {
    // 更新已选择项目的序号
    $('#selectedProjects .task-project-item').each(function(index) {
        $(this).find('.order').text(`#${index + 1}`);
    });

    // 更新未选择项目的序号 (可选)
    $('#unselectedProjects .task-project-item').each(function(index) {
        $(this).find('.order').text('--');  // 未选择的项目可以不显示序号
    });
}

// 时间格式化函数，放在handleTaskSubmit前
function formatDateToYMDHMS(date) {
    const pad = n => n < 10 ? '0' + n : n;
    return date.getFullYear() + '-' +
        pad(date.getMonth() + 1) + '-' +
        pad(date.getDate()) + ' ' +
        pad(date.getHours()) + ':' +
        pad(date.getMinutes()) + ':' +
        pad(date.getSeconds());
}

// 修改任务提交处理函数
function handleTaskSubmit(e) {
    e.preventDefault();
    
    const projectIdsInOrder = $('#selectedProjects .task-project-item').map(function() {
        return $(this).data('id');
    }).get();

    const expertUsernames = $('#selectedExperts .expert-item').map(function() {
        return $(this).data('id');
    }).get();

    if (projectIdsInOrder.length === 0) {
        alert('请选择至少一个项目');
        return;
    }

    if (expertUsernames.length === 0) {
        alert('请选择至少一个评审专家');
        return;
    }

    const taskType = parseInt($('input[name="taskType"]:checked').val());
    const taskCategory = $('#taskCategory').val().trim();
    const scoreGroupTypeRaw = $('input[name="scoreGroupType"]:checked').val();
    let scoreGroupType = scoreGroupTypeRaw;
    if (scoreGroupTypeRaw === 'preliminary') scoreGroupType = 1;
    else if (scoreGroupTypeRaw === 'semifinal') scoreGroupType = 2;
    else if (scoreGroupTypeRaw === 'final') scoreGroupType = 3;
    else scoreGroupType = 1;
    const taskId = $('#taskForm').data('taskId');

    if (!taskCategory) {
        alert('请输入任务类别');
        return;
    }

    if (!scoreGroupType) {
        alert('请选择评分组合');
        return;
    }

    // 构建正确的任务数据格式
    const taskData = {
        category: taskCategory,
        taskType: taskType,
        scoreGroupType: scoreGroupType,
        status: 'pending',
        projectIds: projectIdsInOrder, // 传递项目ID列表
        experts: expertUsernames
    };

    // 如果是新建任务，设置开始时间为当前时间（格式：yyyy-MM-dd HH:mm:ss）
    if (!taskId) {
        taskData.startTime = formatDateToYMDHMS(new Date());
    }

    const apiCall = taskId ? 
        api.updateReviewTask(taskId, taskData) :
        api.createReviewTask(taskData);

    apiCall.then(response => {
        if (response.success) {
            closeTaskDialog();
            loadTasks();
            alert(taskId ? '评审任务更新成功' : '评审任务创建成功');
        }
    }).catch(error => {
        console.error('任务操作失败:', error);
        alert(error.message || (taskId ? '更新任务失败' : '创建任务失败'));
    });
}

// 关闭任务对话框
function closeTaskDialog() {
    $('#taskDialog').fadeOut(200);
}

// 新增：加载任务列表的函数
function loadTasks() {
    const $container = $('#taskListContainer');
    // 保留加载提示，不立即清空
    api.getTasks()
        .then(response => {
            if (response.success) {
                renderTasks(response.data);
            } else {
                $container.html('<p class="error-message">加载任务列表失败</p>');
            }
        })
        .catch(error => {
            console.error('加载任务列表失败:', error);
            $container.html('<p class="error-message">加载任务列表时发生错误</p>');
        });
}

function renderTasks(tasks) {
    const $container = $('#taskListContainer');
    $container.empty(); // 清空"正在加载"提示

    if (!tasks || tasks.length === 0) {
        $container.append('<p>暂无评审任务</p>');
        return;
    }

    // 创建任务列表容器，使用与项目列表相同的网格布局
    const $taskList = $('<div class="task-list"></div>');

    tasks.forEach(task => {
        const actionButtons = `
            ${task.status === 'pending' ? '<button class="btn btn-edit">编辑</button>' : ''}
            ${task.status === 'active' ? '<button class="btn btn-details">详细</button>' : ''}
            ${task.status === 'pending' ? '<button class="btn btn-enable">启用</button>' : ''}
            <button class="btn btn-delete">删除</button>
        `;

        // 处理创建时间显示
        let createTimeText = '未设置';
        if (task.startTime) {
            try {
                createTimeText = new Date(task.startTime).toLocaleString();
            } catch (e) {
                console.warn('无法解析任务开始时间:', task.startTime);
                createTimeText = '时间格式错误';
            }
        }

        // 处理项目数量和专家数量显示
        const projectCount = task.projects && Array.isArray(task.projects) ? task.projects.length : 0;
        const expertCount = task.experts && Array.isArray(task.experts) ? task.experts.length : 0;

        const $card = $(`
            <div class="task-card" data-task-id="${task.id}">
                <h3>${task.category || '未命名任务'}</h3>
                <div class="task-info">
                    <div class="info-item">
                        <label>任务类型：</label>
                        <span>${task.taskType === 1 ? '全部专家完成后进入下一项目' : '个人完成后可进入下一项目'}</span>
                    </div>
                    <div class="info-item">
                        <label>状态：</label>
                        <span>${getReviewTaskStatusText(task.status)}</span>
                    </div>
                    <div class="info-item">
                        <label>开始时间：</label>
                        <span>${createTimeText}</span>
                    </div>
                    <div class="info-item">
                        <label>项目数量：</label>
                        <span>${projectCount} 个</span>
                    </div>
                    <div class="info-item">
                        <label>专家数量：</label>
                        <span>${expertCount} 个</span>
                    </div>
                </div>
                <div class="task-actions">
                    ${actionButtons}
                </div>
            </div>
        `);

        // 绑定按钮事件
        $card.find('.btn-edit').on('click', () => editTask(task));
        $card.find('.btn-details').on('click', () => showTaskDetails(task));
        $card.find('.btn-enable').on('click', () => enableTask(task.id));
        $card.find('.btn-delete').on('click', () => deleteTask(task.id));

        $taskList.append($card);
    });

    $container.append($taskList);
}

// 新增：显示任务详情的函数
async function showTaskDetails(task) {
    await openTaskDialog(task, true);
}

// 编辑任务函数
async function editTask(task) {
    await openTaskDialog(task);
}

function getReviewTaskStatusText(status) {
    const statusMap = {
        'pending': '待启用',
        'active': '已启用',
        'completed': '已完成', // 假设未来可能有此状态
        'inactive': '已禁用'  // 假设未来可能有此状态
    };
    return statusMap[status] || status;
}

// 获取可用的专家角色列表
async function getAvailableRoles() {
    try {
        // 直接返回内置专家角色
        return [
            'expert1', 'expert2', 'expert3', 'expert4', 'expert5', 'expert6', 'expert7'
        ];
    } catch (error) {
        console.error('获取可用角色失败:', error);
        throw error;
    }
}

function importProjects(file) {
    const reader = new FileReader();
    reader.onload = function(e) {
        const projects = JSON.parse(e.target.result);
        projects.forEach(async project => {
            await api.createProject(project);
        });
        loadProjects();
    };
    reader.readAsText(file);
}

// 修改按钮事件绑定
function initializeScoreItemButtons() {
    $('.btn-add-score-item').off('click').on('click', async function() {
        const group = $(this).data('group');
        try {
            const availableRoles = await getAvailableRoles();
            if (!Array.isArray(availableRoles) || availableRoles.length === 0) {
                throw new Error('没有可用的评审专家');
            }
            addScoreItemRow(group, '', [], 0, 100, availableRoles);
        } catch (error) {
            console.error('添加评分项失败:', error);
            alert('添加评分项失败: ' + error.message);
        }
    });
}

function renderProjectsPaginated(projects, page = 1, pageSize = 10) {
    const start = (page - 1) * pageSize;
    const end = start + pageSize;
    const paginatedProjects = projects.slice(start, end);
    
    renderProjects(paginatedProjects);
    renderPagination(projects.length, page, pageSize);
}

function renderPagination(total, currentPage, pageSize) {
    const pages = Math.ceil(total / pageSize);
    // 渲染分页控件
}

// 添加对话框管理函数
function closeDialog() {
    $('#projectDialog').fadeOut(200);
    resetProjectForm();
}

function resetProjectForm() {
    const $form = $('#projectForm');
    $form[0].reset();
    $form.removeData('projectId');
    // 清理全局项目数据
    window.currentProjectData = null;
    // 清空所有评分项组
    ['preliminary', 'semifinal', 'final'].forEach(group => {
        $(`#${group}ScoreItems`).empty();
    });
}

function closeAllDialogs() {
    $('#projectDialog, #templateDialog, #taskDialog').fadeOut(200);
}

// Update the "使用模板" button handler
$('#templateDialog .dialog-buttons button').on('click', async function() { // Make handler async
    await useTemplate(); // Await the async function
});



// 绑定模板选择事件
$(document).on('click', '.template-item', function() {
    $('.template-item').removeClass('selected');
    $(this).addClass('selected');
});

// 还原项目
function restoreProject(projectId) {
    if (!projectId) return;
    api.restoreProject(projectId).then(response => {
        if (response.success) {
            loadProjects();
            alert('项目已还原');
        } else {
            alert(response.message || '还原失败');
        }
    }).catch(error => {
        alert(error.message || '还原失败');
    });
}

// 启用评审任务函数
function enableTask(taskId) {
    if (!taskId) {
        alert('任务ID无效');
        return;
    }
    if (!confirm('确定要启用该评审任务吗？')) {
        return;
    }
    api.enableReviewTask(taskId)
        .then(response => {
            if (response.success) {
                alert('任务已启用');
                loadTasks();
            } else {
                alert(response.message || '启用任务失败');
            }
        })
        .catch(error => {
            alert(error.message || '启用任务失败');
        });
}
