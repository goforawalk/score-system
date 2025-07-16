const auth = {
    setUserInfo: function(userInfo) {
        localStorage.setItem('userInfo', JSON.stringify(userInfo));
    },

    getUserInfo: function() {
        const userInfo = localStorage.getItem('userInfo');
        return userInfo ? JSON.parse(userInfo) : null;
    },

    removeUserInfo: function() {
        localStorage.removeItem('userInfo');
    },

    isLoggedIn: function() {
        return !!this.getUserInfo();
    },

    getRole: function() {
        const userInfo = this.getUserInfo();
        return userInfo ? userInfo.role : null;
    },

    // 新增：大小写不敏感的角色检查函数
    hasRole: function(expectedRole) {
        const currentRole = this.getRole();
        if (!currentRole || !expectedRole) {
            return false;
        }
        return currentRole.toLowerCase() === expectedRole.toLowerCase();
    },

    // 新增：检查是否为管理员
    isAdmin: function() {
        return this.hasRole('admin');
    },

    // 新增：检查是否为专家
    isExpert: function() {
        const role = this.getRole();
        return role && role.toLowerCase().startsWith('expert');
    }
};

// 通用退出方法：弹窗确认是否清空缓存
function logoutWithCacheConfirm() {
    if (confirm('是否清空本地缓存数据？\n选择"确定"将清空所有缓存，避免影响下一次测试。\n选择"取消"将保留缓存数据。')) {
        localStorage.clear();
        console.log('已清空所有缓存数据');
    } else {
        auth.removeUserInfo();
        console.log('仅退出登录，未清空其他缓存');
    }
    window.location.href = '../index.html';
}

// 导出方法（如果有模块化需求）
window.logoutWithCacheConfirm = logoutWithCacheConfirm;