/**
 * 综合验证测试运行脚本
 * 用于快速执行前后端接口一致性验证
 */

const { runComprehensiveVerification } = require('./comprehensive-verification.js');

/**
 * 主函数
 */
async function main() {
    console.log('🚀 开始执行综合验证测试...');
    console.log('='.repeat(60));
    
    try {
        const result = await runComprehensiveVerification();
        
        console.log('\n' + '='.repeat(60));
        if (result) {
            console.log('🎉 所有测试通过！系统接口一致性验证成功！');
            console.log('✅ 前端调用后端服务接口存在且正确');
            console.log('✅ 数据结构格式一致');
            console.log('✅ 响应数据格式符合前端期望');
            process.exit(0);
        } else {
            console.log('⚠️ 部分测试失败，请检查系统实现');
            process.exit(1);
        }
    } catch (error) {
        console.error('❌ 测试执行失败:', error.message);
        process.exit(1);
    }
}

// 如果直接运行此脚本
if (require.main === module) {
    main();
}

module.exports = { main }; 