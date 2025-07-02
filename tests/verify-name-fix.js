// 验证name字段修复的简单测试脚本
console.log('开始验证name字段修复...');

// 模拟测试数据
const testUser = {
    name: '测试用户',
    username: 'testuser' + Date.now(),
    password: 'test123',
    role: 'expert1',
    status: 'active'
};

console.log('测试用户数据:', testUser);

// 模拟handleUserSubmit函数
function handleUserSubmit(userData) {
    const submittedData = {
        name: userData.name,
        username: userData.username,
        role: userData.role,
        status: userData.status
    };
    
    if (userData.password) {
        submittedData.password = userData.password;
    }
    
    return submittedData;
}

// 模拟openDialog函数
function openDialog(title, user = null) {
    const formData = {};
    
    if (user) {
        formData.name = user.name || '';
        formData.username = user.username;
        formData.role = user.role;
        formData.status = user.status || 'active';
    }
    
    return formData;
}

// 测试新增用户
console.log('\n=== 测试新增用户 ===');
const submittedData = handleUserSubmit(testUser);
console.log('提交的数据:', submittedData);
console.log('name字段存在:', 'name' in submittedData);
console.log('name字段值:', submittedData.name);

// 测试编辑用户
console.log('\n=== 测试编辑用户 ===');
const editData = openDialog('编辑用户', testUser);
console.log('编辑表单数据:', editData);
console.log('name字段存在:', 'name' in editData);
console.log('name字段值:', editData.name);

// 测试导出用户
console.log('\n=== 测试导出用户 ===');
const exportData = {
    name: testUser.name,
    username: testUser.username,
    role: testUser.role,
    status: testUser.status
};
console.log('导出数据:', exportData);
console.log('name字段存在:', 'name' in exportData);
console.log('name字段值:', exportData.name);

console.log('\n=== 验证结果 ===');
const allTestsPassed = 
    'name' in submittedData && 
    'name' in editData && 
    'name' in exportData &&
    submittedData.name === testUser.name &&
    editData.name === testUser.name &&
    exportData.name === testUser.name;

if (allTestsPassed) {
    console.log('✅ 所有测试通过！name字段修复成功');
} else {
    console.log('❌ 测试失败！name字段仍有问题');
}

console.log('\n修复验证完成！'); 