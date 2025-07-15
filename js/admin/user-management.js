$(document).ready(function() {
    // 验证用户权限 - 使用新的角色检查函数
    if (!auth.isLoggedIn() || !auth.isAdmin()) {
        console.log('权限验证失败 - 用户角色:', auth.getRole());
        window.location.href = '../index.html';
        return;
    }

    // 初始化页面
    initializeUserInfo();
    loadUsers();
    initBatchOperations(); // 初始化批量操作

    // 修改新增用户按钮事件绑定
    $('#addUserBtn').on('click', function(e) {
        e.preventDefault();
        openDialog('新增用户');
    });

    // 添加关闭对话框的事件绑定
    $('.dialog-content').on('click', function(e) {
        e.stopPropagation();
    });

    $('#userDialog').on('click', function() {
        closeDialog();
    });

    // 绑定表单提交事件
    $('#userForm').on('submit', handleUserSubmit);

    // 全选/取消全选功能
    $(document).on('change', '#selectAllUsers', function() {
        const checked = $(this).prop('checked');
        $('.user-checkbox').prop('checked', checked);
        updateBatchToolbar();
    });

    // 新增：单条checkbox联动全选checkbox
    $(document).on('change', '.user-checkbox', function() {
        updateSelectAllCheckbox();
        updateBatchToolbar();
    });
});

function initializeUserInfo() {
    const userInfo = auth.getUserInfo();
    $('#currentUser').text(userInfo.username);
    $('#logoutBtn').on('click', function(e) {
        e.preventDefault();
        auth.removeUserInfo();
        window.location.href = '../index.html';
    });
}

function loadUsers() {
    api.getUsers()
        .then(response => {
            if (response.success) {
                renderUsers(response.data);
            }
        })
        .catch(error => {
            alert('加载用户列表失败');
            console.error(error);
        });
}

function renderUsers(users) {
    const $userList = $('#userList');
    $userList.empty();

    users.forEach(user => {
        // 处理创建时间
        let createTimeText = '-';
        if (user.createTime) {
            const date = new Date(user.createTime);
            if (!isNaN(date.getTime())) {
                createTimeText = date.toLocaleString();
            }
        }
        const $row = $(
            `<tr data-username="${user.username}">
                <td><input type="checkbox" class="user-checkbox" data-username="${user.username}"></td>
                <td>${user.name}</td>
                <td>${user.username}</td>
                <td>${getRoleText(user.role)}</td>
                <td>${createTimeText}</td>
                <td>
                    <span class="status-badge status-${user.status || 'active'}">
                        ${getStatusText(user.status || 'active')}
                    </span>
                </td>
                <td class="user-actions">
                    <button class="btn btn-edit">编辑</button>
                    <button class="btn btn-delete">删除</button>
                </td>
            </tr>`
        );

        // 绑定操作按钮事件
        $row.find('.btn-edit').on('click', () => editUser(user));
        $row.find('.btn-delete').on('click', () => deleteUser(user.username));

        $userList.append($row);
    });

    // 渲染后，更新全选checkbox状态
    updateSelectAllCheckbox();
    updateBatchToolbar(); // 更新批量操作工具栏
}

function getRoleText(role) {
    const roleMap = {
        'admin': '管理员',
        'expert1': '评审专家1',
        'expert2': '评审专家2',
        'expert3': '评审专家3',
        'expert4': '评审专家4',
        'expert5': '评审专家5',
        'expert6': '评审专家6',
        'expert7': '评审专家7'
    };
    return roleMap[role] || role;
}

function getStatusText(status) {
    const statusMap = {
        'active': '启用',
        'inactive': '禁用'
    };
    return statusMap[status] || status;
}

function openDialog(title, user = null) {
    $('#dialogTitle').text(title);
    const $form = $('#userForm');
    $form[0].reset();

    if (user) {
        $form.data('username', user.username);
        $('#name').val(user.name || '');
        $('#username').val(user.username).prop('readonly', true);
        $('#role').val(user.role);
        $('#status').val(user.status || 'active');
        $('#password').prop('required', false);
    } else {
        $form.removeData('username');
        $('#username').prop('readonly', false);
        $('#password').prop('required', true);
    }

    $('#userDialog').fadeIn(200);
}

function closeDialog() {
    const $dialog = $('#userDialog');
    $dialog.fadeOut(200);
    $('#userForm')[0].reset();
}

function handleUserSubmit(e) {
    e.preventDefault();

    const username = $('#userForm').data('username');
    const userData = {
        name: $('#name').val(),
        username: $('#username').val(),
        role: $('#role').val(),
        status: $('#status').val()
    };

    // 如果输入了密码，添加到数据中
    const password = $('#password').val();
    if (password) {
        userData.password = password;
    }

    const apiCall = username
        ? api.updateUser(username, userData)
        : api.addUser(userData);

    // 显示加载状态
    const $submitBtn = $('#userForm button[type="submit"]');
    const originalText = $submitBtn.text();
    $submitBtn.prop('disabled', true).text('保存中...');

    apiCall
        .then(response => {
            if (response.success) {
                // 显示成功提示
                showMessage(username ? '用户信息更新成功！' : '用户创建成功！', 'success');
                closeDialog();
                loadUsers();
            } else {
                showMessage(response.message || '保存失败', 'error');
            }
        })
        .catch(error => {
            showMessage(error.message || '保存失败', 'error');
        })
        .finally(() => {
            // 恢复按钮状态
            $submitBtn.prop('disabled', false).text(originalText);
        });
}

// 新增：显示消息提示函数
function showMessage(message, type = 'info') {
    // 移除现有的消息提示
    $('.message-toast').remove();
    
    const toastClass = type === 'success' ? 'message-toast success' : 
                      type === 'error' ? 'message-toast error' : 
                      'message-toast info';
    
    const $toast = $(`
        <div class="${toastClass}">
            <span class="message-text">${message}</span>
            <button class="close-btn">&times;</button>
        </div>
    `);
    
    // 添加到页面
    $('body').append($toast);
    
    // 显示动画
    $toast.fadeIn(300);
    
    // 绑定关闭事件
    $toast.find('.close-btn').on('click', function() {
        $toast.fadeOut(300, function() {
            $(this).remove();
        });
    });
    
    // 自动隐藏（成功消息3秒后自动隐藏，错误消息需要手动关闭）
    if (type === 'success') {
        setTimeout(() => {
            $toast.fadeOut(300, function() {
                $(this).remove();
            });
        }, 3000);
    }
}

function editUser(user) {
    openDialog('编辑用户', user);
}

function deleteUser(username) {
    if (username === 'admin') {
        showMessage('不能删除管理员账号', 'error');
        return;
    }

    if (confirm('确定要删除此用户吗？')) {
        api.deleteUser(username)
            .then(response => {
                if (response.success) {
                    showMessage('用户删除成功！', 'success');
                    loadUsers();
                } else {
                    showMessage(response.message || '删除失败', 'error');
                }
            })
            .catch(error => {
                showMessage(error.message || '删除失败', 'error');
            });
    }
}

function importUsers(file) {
    const reader = new FileReader();
    reader.onload = async function(e) {
        const users = JSON.parse(e.target.result);
        for (const user of users) {
            try {
                await api.addUser(user);
            } catch (error) {
                console.error(`Failed to import user ${user.username}:`, error);
            }
        }
        loadUsers();
    };
    reader.readAsText(file);
}

function exportUsers() {
    api.getUsers()
        .then(response => {
            if (response.success) {
                const users = response.data.map(user => ({
                    name: user.name,
                    username: user.username,
                    role: user.role,
                    status: user.status
                }));
                const blob = new Blob([JSON.stringify(users, null, 2)], { type: 'application/json' });
                const url = URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = 'users.json';
                a.click();
            }
        });
}

function initBatchOperations() {
    // 添加批量操作按钮
    $('.header-actions').prepend(`
        <button id="batchActionBtn" class="btn">批量操作</button>
    `);

    // 添加批量操作工具栏
    $('.header').after(`
        <div class="batch-toolbar" style="display: none;">
            <div class="selected-count">已选择 <span id="selectedCount">0</span> 个用户</div>
            <div class="batch-actions">
                <button class="btn btn-batch-disable">批量禁用</button>
                <button class="btn btn-batch-delete">批量删除</button>
                <button class="btn btn-batch-cancel">取消</button>
            </div>
        </div>
    `);

    // 绑定事件处理
    $('#batchActionBtn').on('click', toggleBatchMode);
    $('.btn-batch-disable').on('click', handleBatchDisable);
    $('.btn-batch-delete').on('click', handleBatchDelete);
    $('.btn-batch-cancel').on('click', exitBatchMode);
}

function toggleBatchMode() {
    const $toolbar = $('.batch-toolbar');
    const isVisible = $toolbar.is(':visible');

    if (isVisible) {
        exitBatchMode();
    } else {
        $toolbar.fadeIn(200);
        $('#batchActionBtn').text('完成').removeClass('btn-primary').addClass('btn-danger');
    }
}

function exitBatchMode() {
    $('.batch-toolbar').fadeOut(200);
    $('#batchActionBtn').text('批量操作').removeClass('btn-danger').addClass('btn-primary');
    $('.user-checkbox').prop('checked', false);
    updateBatchToolbar();
}

function handleBatchDisable() {
    const selectedUsers = getSelectedUsers();
    if (selectedUsers.length === 0) {
        showMessage('请先选择用户', 'error');
        return;
    }

    if (confirm(`确定要禁用这 ${selectedUsers.length} 个用户吗？`)) {
        let successCount = 0;
        let errorCount = 0;
        
        const promises = selectedUsers.map(username => 
            api.updateUser(username, { status: 'inactive' })
                .then(response => {
                    if (response.success) {
                        successCount++;
                    } else {
                        errorCount++;
                    }
                })
                .catch(error => {
                    errorCount++;
                    console.error(`禁用用户 ${username} 失败:`, error);
                })
        );

        Promise.all(promises).then(() => {
            if (errorCount === 0) {
                showMessage(`成功禁用 ${successCount} 个用户！`, 'success');
            } else {
                showMessage(`批量禁用完成：成功 ${successCount} 个，失败 ${errorCount} 个`, 'info');
            }
            loadUsers();
            exitBatchMode();
        });
    }
}

function handleBatchDelete() {
    const selectedUsers = getSelectedUsers();
    if (selectedUsers.length === 0) {
        showMessage('请先选择用户', 'error');
        return;
    }

    if (confirm(`确定要删除这 ${selectedUsers.length} 个用户吗？`)) {
        let successCount = 0;
        let errorCount = 0;
        
        const promises = selectedUsers.map(username => 
            api.deleteUser(username)
                .then(response => {
                    if (response.success) {
                        successCount++;
                    } else {
                        errorCount++;
                    }
                })
                .catch(error => {
                    errorCount++;
                    console.error(`删除用户 ${username} 失败:`, error);
                })
        );

        Promise.all(promises).then(() => {
            if (errorCount === 0) {
                showMessage(`成功删除 ${successCount} 个用户！`, 'success');
            } else {
                showMessage(`批量删除完成：成功 ${successCount} 个，失败 ${errorCount} 个`, 'info');
            }
            loadUsers();
            exitBatchMode();
        });
    }
}

function getSelectedUsers() {
    return $('.user-checkbox:checked').map(function() {
        return $(this).data('username');
    }).get();
}

function updateBatchToolbar() {
    const selectedCount = $('.user-checkbox:checked').length;
    $('#selectedCount').text(selectedCount);

    const $toolbar = $('.batch-toolbar');
    if (selectedCount > 0) {
        $toolbar.show();
    } else {
        $toolbar.hide();
    }
}

function updateSelectAllCheckbox() {
    const total = $('.user-checkbox').length;
    const checked = $('.user-checkbox:checked').length;
    $('#selectAllUsers').prop('checked', total > 0 && total === checked);
}

