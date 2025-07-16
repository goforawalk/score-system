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
        logoutWithCacheConfirm();
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