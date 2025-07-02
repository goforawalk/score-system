$(document).ready(function() {
    // 验证用户权限 - 使用新的角色检查函数
    if (!auth.isLoggedIn() || !auth.isAdmin()) {
        console.log('权限验证失败 - 用户角色:', auth.getRole());
        window.location.href = '../index.html';
        return;
    }

    // 显示当前用户
    const userInfo = auth.getUserInfo();
    $('#currentUser').text(userInfo.username);

    // 退出登录
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

    // 加载统计数据
    loadDashboardStats();
});

function loadDashboardStats() {
    api.getProjectStats()
        .then(response => {
            if (response.success) {
                $('#totalProjects').text(response.data.totalProjects);
                $('#activeProjects').text(response.data.activeProjects);
                $('#totalExperts').text(response.data.totalExperts);
            }
        })
        .catch(error => {
            alert('加载统计数据失败');
            console.error(error);
        });
}