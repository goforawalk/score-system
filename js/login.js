$(document).ready(function() {
    // 添加记住用户名功能
    if (localStorage.getItem('rememberedUsername')) {
        $('#username').val(localStorage.getItem('rememberedUsername'));
        $('#rememberUsername').prop('checked', true);
    }
    
    // 如果已登录，直接跳转到对应页面
    if (auth.isLoggedIn()) {
        const role = auth.getRole();
        console.log('用户已登录，角色:', role);
        redirectByRole(role);
        return;
    }

    $('#loginForm').on('submit', function(e) {
        e.preventDefault();
        
        const username = $('#username').val().trim();
        const password = $('#password').val().trim();
        
        // 添加表单验证提示
        if (!username) {
            showError('请输入用户名');
            return;
        }
        if (!password) {
            showError('请输入密码');
            return;
        }
        
        // 添加loading状态
        const $btn = $(this).find('button[type="submit"]');
        $btn.prop('disabled', true).text('登录中...');
        
        // 调用API适配器登录接口
        api.login(username, password)
            .then(response => {
                console.log('登录响应数据:', response);
                console.log('响应类型:', typeof response);
                console.log('响应success字段:', response.success);
                
                if (response.success) {
                    console.log('登录成功，用户信息:', response.data);
                    auth.setUserInfo(response.data);
                    // 登录成功后保存用户名
                    if ($('#rememberUsername').is(':checked')) {
                        localStorage.setItem('rememberUsername', username);
                    } else {
                        localStorage.removeItem('rememberUsername');
                    }
                    console.log('准备跳转到角色页面，角色:', response.data.role);
                    redirectByRole(response.data.role);
                } else {
                    console.error('登录失败，响应:', response);
                    showError(response.message || '登录失败');
                }
            })
            .catch(error => {
                console.error('登录请求异常:', error);
                showError(error.message || '登录失败');
            })
            .finally(() => {
                // 恢复按钮状态
                $btn.prop('disabled', false).text('登录');
            });
    });
    
    function redirectByRole(role) {
        // 将角色转换为小写进行比较，以处理大小写不匹配问题
        const roleLower = role ? role.toLowerCase() : '';
        console.log('角色匹配检查 - 原始角色:', role, '转换为小写:', roleLower);
        
        if (roleLower === 'admin') {
            console.log('跳转到管理员页面');
            window.location.href = 'admin/dashboard.html';
        } else if (roleLower.startsWith('expert')) {
            console.log('跳转到专家页面');
            window.location.href = 'expert/scoring.html';
        } else {
            console.error('未知角色:', role);
            showError('未知的用户角色: ' + role);
        }
    }

    // 添加错误提示函数
    function showError(message) {
        const $error = $('.error-message');
        if ($error.length === 0) {
            $('<div class="error-message"></div>').insertAfter('#loginForm');
        }
        $('.error-message').text(message).fadeIn();
    }
});