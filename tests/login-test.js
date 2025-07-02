// 测试新增用户后的登录功能
console.log('开始测试新增用户登录功能...');

// 模拟用户数据
const testUser = {
    name: '测试用户',
    username: 'testuser' + Date.now(),
    password: 'test123',
    role: 'expert1',
    status: 'active'
};

console.log('测试用户数据:', testUser);

// 模拟mockApi对象
const mockApi = {
    users: [
        { username: 'admin', name: '管理员', password: 'admin123', role: 'admin' },
        { username: 'expert1', name: '评审专家1', password: 'expert123', role: 'expert1' }
    ],

    addUser: function(userData) {
        return new Promise((resolve, reject) => {
            setTimeout(() => {
                if (this.users.some(u => u.username === userData.username)) {
                    reject({
                        success: false,
                        message: '用户名已存在'
                    });
                    return;
                }
                
                const newUser = {
                    ...userData,
                    createTime: new Date().toISOString()
                };
                this.users.push(newUser);
                
                console.log('新增用户后的用户列表:', this.users.map(u => ({ username: u.username, password: u.password })));
                
                resolve({
                    success: true,
                    data: {
                        ...newUser,
                        password: undefined
                    }
                });
            }, 100);
        });
    },

    login: function(username, password) {
        return new Promise((resolve, reject) => {
            setTimeout(() => {
                console.log('登录验证 - 用户名:', username, '密码:', password);
                console.log('当前用户列表:', this.users.map(u => ({ username: u.username, password: u.password })));
                
                const user = this.users.find(u => 
                    u.username === username && u.password === password
                );
                
                if (user) {
                    resolve({
                        success: true,
                        data: {
                            username: user.username,
                            role: user.role,
                            token: 'mock-jwt-token-' + Date.now()
                        }
                    });
                } else {
                    reject({
                        success: false,
                        message: '用户名或密码错误'
                    });
                }
            }, 100);
        });
    }
};

// 测试流程
async function testUserCreationAndLogin() {
    try {
        console.log('\n=== 步骤1：新增用户 ===');
        const addResult = await mockApi.addUser(testUser);
        console.log('新增用户结果:', addResult.success ? '成功' : '失败');
        
        if (addResult.success) {
            console.log('\n=== 步骤2：尝试登录 ===');
            const loginResult = await mockApi.login(testUser.username, testUser.password);
            console.log('登录结果:', loginResult.success ? '成功' : '失败');
            
            if (loginResult.success) {
                console.log('登录成功，用户信息:', loginResult.data);
            } else {
                console.log('登录失败:', loginResult.message);
            }
        }
        
        console.log('\n=== 最终用户列表 ===');
        console.log(mockApi.users.map(u => ({ username: u.username, password: u.password, role: u.role })));
        
    } catch (error) {
        console.error('测试过程中出现错误:', error);
    }
}

// 运行测试
testUserCreationAndLogin(); 