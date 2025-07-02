$(document).ready(function() {
    // 验证用户权限 - 使用新的角色检查函数
    if (!auth.isLoggedIn() || !auth.isAdmin()) {
        console.log('权限验证失败 - 用户角色:', auth.getRole());
        window.location.href = '../index.html';
        return;
    }

    // 初始化页面
    initializeUserInfo();
    loadProjects();
    loadStatistics();

    // 绑定项目选择事件
    $('#projectSelect').on('change', function() {
        const projectId = $(this).val();
        if (projectId) {
            try {
                // 从缓存中获取数据
                const cachedData = statisticsCache.get(projectId);
                if (cachedData) {
                    console.log('使用缓存数据更新图表');
                    updateChartsWithData(cachedData);
                    return;
                }

                showLoading();
                updateCharts(projectId)
                    .then(() => {
                        console.log('图表更新成功');
                    })
                    .catch(error => {
                        console.error('Update charts error:', error);
                        showError('更新图表失败: ' + error.message);
                        // 清除缓存
                        statisticsCache.data.delete(projectId);
                    })
                    .finally(() => {
                        hideLoading();
                    });
            } catch (error) {
                console.error('Error handling project selection:', error);
                showError('处理项目选择时出错');
                // 清除缓存
                statisticsCache.data.delete(projectId);
            }
        } else {
            clearCharts();
        }
    });

    // 绑定导出按钮事件
    $('#exportExcel').on('click', handleExportExcel);
    $('#exportPDF').on('click', handleExportPDF);
});

// 修改 statisticsCache 对象的 get 方法
const statisticsCache = {
    data: new Map(),
    timeout: 5 * 60 * 1000, // 5分钟缓存

    set(projectId, data) {
        this.data.set(projectId, {
            timestamp: Date.now(),
            data: JSON.parse(JSON.stringify(data)) // 深拷贝数据
        });
    },

    get(projectId) {
        const cached = this.data.get(projectId);
        if (!cached) return null;
        if (Date.now() - cached.timestamp > this.timeout) {
            this.data.delete(projectId);
            return null;
        }
        return JSON.parse(JSON.stringify(cached.data)); // 返回深拷贝
    }
};

const reportTemplates = {
    basic: {
        name: '基础报表',
        sections: ['projectInfo', 'scoreOverview', 'expertScores']
    },
    detailed: {
        name: '详细报表',
        sections: ['projectInfo', 'scoreOverview', 'expertScores', 'distribution', 'progress']
    }
};

function initializeUserInfo() {
    const userInfo = auth.getUserInfo();
    $('#currentUser').text(userInfo.username);
    $('#logoutBtn').on('click', function(e) {
        e.preventDefault();
        auth.removeUserInfo();
        window.location.href = '../index.html';
    });
}

function loadProjects() {
    api.getProjects()
        .then(response => {
            if (response.success) {
                const $select = $('#projectSelect');
                $select.find('option:not(:first)').remove();
                
                // 只显示状态为 active 的项目
                const activeProjects = response.data.filter(p => p.status === 'active');
                
                activeProjects.forEach(project => {
                    $select.append(`
                        <option value="${project.id}">${project.name}</option>
                    `);
                });
                
                if (activeProjects.length === 0) {
                    $select.append('<option disabled>暂无可用项目</option>');
                }
            }
        })
        .catch(error => {
            alert('加载项目列表失败');
            console.error(error);
        });
}

function loadStatistics() {
    mockApi.getStatistics()
        .then(response => {
            if (response.success) {
                renderStatisticsTable(response.data);
                initializeCharts(response.data);
            }
        })
        .catch(error => {
            alert('加载统计数据失败');
            console.error(error);
        });
}

function renderStatisticsTable(statistics) {
    const $tbody = $('#statisticsData');
    $tbody.empty();

    statistics.forEach(stat => {
        const rows = stat.itemStats.map((item, index) => `
            <tr>
                <td>${index === 0 ? stat.name : ''}</td>
                <td>${index === 0 ? stat.totalScore.toFixed(2) : ''}</td>
                <td>${item.name}</td>
                <td>${item.avgScore.toFixed(2)}</td>
                <td>${item.maxScore}</td>
                <td>${item.minScore}</td>
                <td>
                    <div class="completion-rate">
                        <div class="completion-bar">
                            <div class="completion-progress" style="width: ${stat.completionRate}%"></div>
                        </div>
                        <span>${stat.completionRate.toFixed(1)}%</span>
                    </div>
                </td>
            </tr>
        `);

        $tbody.append(rows.join(''));
    });
}

function initializeCharts(statistics) {
    // 使用 requestAnimationFrame 优化渲染
    requestAnimationFrame(() => {
        initRankingChart(statistics);
        requestAnimationFrame(() => {
            initProgressChart(statistics);
            requestAnimationFrame(() => {
                initDistributionChart(statistics);
                requestAnimationFrame(() => {
                    initComparisonChart(statistics);
                });
            });
        });
    });
}

function initRankingChart(statistics) {
    const chart = echarts.init(document.getElementById('rankingChart'));
    const data = statistics.map(stat => ({
        name: stat.name,
        value: stat.totalScore
    })).sort((a, b) => b.value - a.value);

    const option = {
        tooltip: {
            trigger: 'axis',
            formatter: '{b}: {c}分'
        },
        xAxis: {
            type: 'value',
            name: '得分'
        },
        yAxis: {
            type: 'category',
            data: data.map(item => item.name),
            axisLabel: {
                interval: 0,
                rotate: 30
            }
        },
        series: [{
            type: 'bar',
            data: data.map(item => item.value),
            itemStyle: {
                color: '#1890ff'
            }
        }]
    };
    chart.setOption(option);
}

function initProgressChart(statistics) {
    const chart = echarts.init(document.getElementById('progressChart'));
    const data = statistics.map(stat => ({
        name: stat.name,
        value: stat.completionRate
    }));

    const option = {
        tooltip: {
            trigger: 'item',
            formatter: '{b}: {c}%'
        },
        series: [{
            type: 'pie',
            radius: ['50%', '70%'],
            data: data.map(item => ({
                name: item.name,
                value: item.value
            })),
            label: {
                show: true,
                formatter: '{b}: {d}%'
            }
        }]
    };
    chart.setOption(option);
}

function initDistributionChart(statistics) {
    const chart = echarts.init(document.getElementById('distributionChart'));
    const allScores = statistics.flatMap(stat => 
        stat.itemStats.map(item => item.avgScore)
    );

    const option = {
        tooltip: {
            trigger: 'axis'
        },
        xAxis: {
            type: 'category',
            data: ['0-60', '60-70', '70-80', '80-90', '90-100'],
            name: '分数区间'
        },
        yAxis: {
            type: 'value',
            name: '数量'
        },
        series: [{
            type: 'bar',
            data: calculateScoreDistribution(allScores),
            itemStyle: {
                color: '#1890ff'
            }
        }]
    };
    chart.setOption(option);
}

function initComparisonChart(statistics) {
    const chart = echarts.init(document.getElementById('comparisonChart'));
    const projects = statistics.map(stat => stat.name);
    const series = [];
    
    // 获取所有评分项名称
    const itemNames = new Set();
    statistics.forEach(stat => {
        stat.itemStats.forEach(item => itemNames.add(item.name));
    });

    // 为每个评分项创建系列数据
    itemNames.forEach(itemName => {
        const data = statistics.map(stat => {
            const item = stat.itemStats.find(i => i.name === itemName);
            return item ? item.avgScore : 0;
        });

        series.push({
            name: itemName,
            type: 'line',
            data: data
        });
    });

    const option = {
        tooltip: {
            trigger: 'axis'
        },
        legend: {
            data: Array.from(itemNames),
            orient: 'vertical',
            right: 10,
            top: 'center'
        },
        xAxis: {
            type: 'category',
            data: projects,
            axisLabel: {
                interval: 0,
                rotate: 30
            }
        },
        yAxis: {
            type: 'value',
            name: '平均分'
        },
        series: series
    };
    chart.setOption(option);
}

function calculateScoreDistribution(scores) {
    const distribution = [0, 0, 0, 0, 0]; // 对应5个分数区间
    scores.forEach(score => {
        if (score < 60) distribution[0]++;
        else if (score < 70) distribution[1]++;
        else if (score < 80) distribution[2]++;
        else if (score < 90) distribution[3]++;
        else distribution[4]++;
    });
    return distribution;
}

function updateCharts(projectId) {
    console.log('开始更新图表，项目ID:', projectId);
    return new Promise((resolve, reject) => {
        if (!projectId) {
            reject(new Error('请选择项目'));
            return;
        }

        mockApi.getProjectScores(projectId)
            .then(response => {
                console.log('获取到项目数据:', response);
                if (response.success) {
                    const data = response.data;
                    try {
                        updateChartsWithData(data);
                        // 更新成功后缓存数据
                        statisticsCache.set(projectId, data);
                        resolve();
                    } catch (error) {
                        reject(new Error('图表更新失败: ' + error.message));
                    }
                } else {
                    reject(new Error(response.message || '获取数据失败'));
                }
            })
            .catch(error => {
                reject(new Error('获取项目数据失败: ' + error.message));
            });
    });
}

function clearCharts() {
    ['rankingChart', 'progressChart', 'distributionChart', 'comparisonChart']
        .forEach(id => {
            const chart = echarts.getInstanceByDom(document.getElementById(id));
            if (chart) {
                chart.clear();
            }
        });
}

async function handleExportExcel() {
    const projectId = $('#projectSelect').val();
    if (!projectId) {
        alert('请先选择项目');
        return;
    }

    showExportProgress(0);
    try {
        const response = await mockApi.exportToExcel(projectId);
        if (response.success) {
            showExportProgress(50);
            const workbook = generateExcelWorkbook(response.data);
            showExportProgress(80);
            await saveExcelFile(workbook);
            showExportProgress(100);
        }
    } catch (error) {
        alert('导出失败：' + error.message);
    }
}

function generateExcelFile(data) {
    // 创建工作簿
    const wb = XLSX.utils.book_new();
    
    // 项目基本信息工作表
    const basicInfo = [
        ['项目名称', data.projectName],
        ['导出时间', new Date(data.exportTime).toLocaleString()],
        ['平均得分', data.statistics.averageScore.toFixed(2)]
    ];
    const wsBasic = XLSX.utils.aoa_to_sheet(basicInfo);
    XLSX.utils.book_append_sheet(wb, wsBasic, "项目信息");

    // 评分详情工作表
    const scoreHeaders = ['评审专家', '评分项', '得分', '权重', '加权得分', '提交时间'];
    const scoreData = [scoreHeaders];
    
    data.scores.forEach(score => {
        score.scores.forEach(item => {
            scoreData.push([
                score.username,
                item.name,
                item.score,
                item.weight + '%',
                (item.score * item.weight / 100).toFixed(2),
                new Date(score.submitTime).toLocaleString()
            ]);
        });
    });
    
    const wsScores = XLSX.utils.aoa_to_sheet(scoreData);
    XLSX.utils.book_append_sheet(wb, wsScores, "评分详情");

    // 导出文件
    const fileName = `${data.projectName}-评分详情-${new Date().toISOString().split('T')[0]}.xlsx`;
    XLSX.writeFile(wb, fileName);
}

function handleExportPDF() {
    const projectId = $('#projectSelect').val();
    if (!projectId) {
        alert('请先选择项目');
        return;
    }

    mockApi.generatePDFReport(projectId)
        .then(response => {
            if (response.success) {
                const data = response.data;
                generatePDFReport(data);
            }
        })
        .catch(error => {
            alert('生成PDF报告失败');
            console.error(error);
        });
}

function generatePDFReport(data) {
    // 创建临时的报告内容
    const $report = $('<div>').addClass('pdf-report').html(`
        <div class="report-header">
            <h1>${data.projectName} - 评审报告</h1>
            <p>生成时间：${new Date(data.generateTime).toLocaleString()}</p>
        </div>
        <div class="report-summary">
            <h2>评审概况</h2>
            <p>总评审专家数：${data.summary.totalExperts}</p>
            <p>已完成评审数：${data.summary.completedScoring}</p>
            <p>项目平均得分：${data.summary.averageScore.toFixed(2)}</p>
        </div>
        <div class="report-charts">
            <h2>统计图表</h2>
            <div id="pdfCharts">
                <!-- 图表将被动态插入 -->
            </div>
        </div>
    `);

    // 将报告内容添加到页面（隐藏）
    $report.css({
        position: 'fixed',
        left: '-9999px',
        top: 0
    }).appendTo('body');

    // 复制当前图表到报告中
    const $chartsContainer = $report.find('#pdfCharts');
    ['rankingChart', 'progressChart', 'distributionChart', 'comparisonChart'].forEach(chartId => {
        const $canvas = $(document.getElementById(chartId)).clone();
        $chartsContainer.append($canvas);
    });

    // 使用html2canvas将报告转换为图片
    html2canvas($report[0]).then(canvas => {
        const imgData = canvas.toDataURL('image/png');
        
        // 创建PDF文档
        const pdf = new jsPDF('p', 'mm', 'a4');
        const pdfWidth = pdf.internal.pageSize.getWidth();
        const pdfHeight = pdf.internal.pageSize.getHeight();
        
        // 添加图片到PDF
        pdf.addImage(imgData, 'PNG', 0, 0, pdfWidth, pdfHeight);
        
        // 保存PDF
        const fileName = `${data.projectName}-评审报告-${new Date().toISOString().split('T')[0]}.pdf`;
        pdf.save(fileName);
        
        // 移除临时报告内容
        $report.remove();
    });
}

// 显示加载状态
function showLoading() {
    $('.loading-overlay').fadeIn(200);
}

// 隐藏加载状态
function hideLoading() {
    $('.loading-overlay').fadeOut(200);
}

// 显示导出进度
function showExportProgress(progress) {
    const $progress = $('.export-progress');
    $progress.find('.progress-bar-inner').css('width', `${progress}%`);
    $progress.fadeIn(200);
    
    if (progress >= 100) {
        setTimeout(() => {
            $progress.fadeOut(200);
            setTimeout(() => {
                $progress.find('.progress-bar-inner').css('width', '0%');
            }, 200);
        }, 1000);
    }
}

// 下载图表为图片
function downloadChart(chart, fileName) {
    const url = chart.getDataURL();
    const a = document.createElement('a');
    a.download = fileName;
    a.href = url;
    a.click();
}

// 绑定图表操作按钮事件
function bindChartActions() {
    $('.chart-card').each(function() {
        const $card = $(this);
        const chartId = $card.find('.chart').attr('id');
        const chart = echarts.getInstanceByDom(document.getElementById(chartId));
        
        // 刷新按钮
        $card.find('.chart-action-btn[title="刷新数据"]').on('click', function() {
            const projectId = $('#projectSelect').val();
            if (projectId) {
                showLoading();
                updateCharts(projectId).finally(hideLoading);
            }
        });
        
        // 下载按钮
        $card.find('.chart-action-btn[title="下载图表"]').on('click', function() {
            const fileName = `${chartId}-${new Date().toISOString().split('T')[0]}.png`;
            downloadChart(chart, fileName);
        });
    });
}

function customizeReport() {
    const $dialog = $(`
        <div class="dialog">
            <div class="dialog-content">
                <h3>自定义报表</h3>
                <div class="report-sections">
                    ${Object.entries(reportSections).map(([key, section]) => `
                        <label>
                            <input type="checkbox" name="sections" value="${key}">
                            ${section.name}
                        </label>
                    `).join('')}
                </div>
                <div class="dialog-buttons">
                    <button type="button" class="btn" onclick="generateCustomReport()">生成报表</button>
                    <button type="button" class="btn btn-cancel" onclick="closeDialog()">取消</button>
                </div>
            </div>
        </div>
    `);
    $('body').append($dialog);
}

function updateRankingChart(data) {
    const chart = echarts.getInstanceByDom(document.getElementById('rankingChart'));
    if (!chart) return;

    const chartData = [{
        name: data.projectName,
        value: data.statistics.averageScore
    }];

    chart.setOption({
        series: [{
            data: chartData
        }]
    });
}

function updateProgressChart(data) {
    const chart = echarts.getInstanceByDom(document.getElementById('progressChart'));
    if (!chart) return;

    const progressData = {
        name: data.projectName,
        value: (data.progress.scored / data.progress.total * 100).toFixed(1)
    };

    chart.setOption({
        series: [{
            data: [progressData]
        }]
    });
}

function updateDistributionChart(data) {
    const chart = echarts.getInstanceByDom(document.getElementById('distributionChart'));
    if (!chart) return;

    const scores = data.details.map(detail => detail.score);
    const distribution = calculateScoreDistribution(scores);

    chart.setOption({
        series: [{
            data: distribution
        }]
    });
}

function updateComparisonChart(data) {
    const chart = echarts.getInstanceByDom(document.getElementById('comparisonChart'));
    if (!chart) return;

    // 按评分项分组计算平均分
    const itemStats = {};
    data.details.forEach(detail => {
        detail.scores.forEach(score => {
            if (!itemStats[score.itemName]) {
                itemStats[score.itemName] = {
                    total: 0,
                    count: 0
                };
            }
            itemStats[score.itemName].total += score.score;
            itemStats[score.itemName].count++;
        });
    });

    const series = Object.entries(itemStats).map(([name, stats]) => ({
        name: name,
        type: 'line',
        data: [stats.total / stats.count]
    }));

    chart.setOption({
        series: series
    });
}

// 修改 updateChartsWithData 函数
function updateChartsWithData(data) {
    try {
        // 检查数据有效性
        if (!data || !data.statistics || !data.progress || !data.details) {
            throw new Error('无效的数据格式');
        }

        // 清除现有图表
        clearCharts();

        // 更新各个图表
        ['rankingChart', 'progressChart', 'distributionChart', 'comparisonChart'].forEach(chartId => {
            const chart = echarts.getInstanceByDom(document.getElementById(chartId));
            if (chart) {
                chart.clear();
            }
        });

        // 重新渲染图表
        updateRankingChart(data);
        updateProgressChart(data);
        updateDistributionChart(data);
        updateComparisonChart(data);

        // 所有图表更新后重新调整大小
        ['rankingChart', 'progressChart', 'distributionChart', 'comparisonChart'].forEach(chartId => {
            const chart = echarts.getInstanceByDom(document.getElementById(chartId));
            if (chart) {
                chart.resize();
            }
        });

    } catch (error) {
        console.error('更新图表出错:', error);
        showError('更新图表失败: ' + error.message);
        
        // 清除缓存中的错误数据
        const projectId = $('#projectSelect').val();
        if (projectId) {
            statisticsCache.data.delete(projectId);
        }
        
        // 重新获取数据
        updateCharts(projectId);
    }
}

// 添加图表调整大小的监听
$(window).on('resize', function() {
    ['rankingChart', 'progressChart', 'distributionChart', 'comparisonChart'].forEach(chartId => {
        const chart = echarts.getInstanceByDom(document.getElementById(chartId));
        if (chart) {
            chart.resize();
        }
    });
});