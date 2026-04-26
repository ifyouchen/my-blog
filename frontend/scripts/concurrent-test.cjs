/**
 * Concurrent API Test Script
 * Run: node scripts/concurrent-test.cjs
 *
 * Tests concurrent API performance and generates report
 */

const API_BASE = 'http://localhost:8080/api';

// Test configuration
const CONCURRENCY_LEVELS = [10, 50, 100];
const REQUESTS_PER_LEVEL = 100;

const TEST_ENDPOINTS = [
    { name: 'GET /api/articles (list)', method: 'GET', path: '/articles?page=1&pageSize=10' },
    { name: 'GET /api/articles/:id (detail)', method: 'GET', path: '/articles/1' },
    { name: 'GET /api/categories', method: 'GET', path: '/categories' },
    { name: 'GET /api/tags', method: 'GET', path: '/tags' },
    { name: 'GET /api/rankings/articles', method: 'GET', path: '/rankings/articles?limit=10' },
    { name: 'GET /api/rankings/authors', method: 'GET', path: '/rankings/authors?limit=10' },
    { name: 'GET /api/home/stats', method: 'GET', path: '/home/stats' },
    { name: 'GET /api/columns?page=1&pageSize=9', method: 'GET', path: '/columns?page=1&pageSize=9' },
    { name: 'GET /api/users/me (authenticated)', method: 'GET', path: '/users/me', auth: true },
    { name: 'POST /api/articles (create)', method: 'POST', path: '/articles', auth: true, body: { title: 'Test', summary: 'Test', content: 'Test', category: 'Java', tags: ['test'], status: 'PUBLISHED' } },
];

// State
let authToken = null;
const stats = {};

// Helpers
function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

function percentile(values, p) {
    const sorted = [...values].sort((a, b) => a - b);
    const idx = Math.ceil(sorted.length * p / 100) - 1;
    return sorted[Math.max(0, idx)];
}

function formatMs(ms) {
    if (ms < 1000) return `${Math.round(ms)}ms`;
    return `${(ms / 1000).toFixed(2)}s`;
}

async function apiRequest(endpoint, concurrency) {
    const startTime = Date.now();
    let status = 0;
    let success = false;
    let error = null;

    const headers = {
        'Content-Type': 'application/json',
    };

    if (endpoint.auth && authToken) {
        headers['Authorization'] = `Bearer ${authToken}`;
    }

    try {
        const options = {
            method: endpoint.method,
            headers,
        };

        if (endpoint.body) {
            options.body = JSON.stringify(endpoint.body);
        }

        const response = await fetch(`${API_BASE}${endpoint.path}`, options);
        status = response.status;

        if (response.ok) {
            await response.json();
            success = true;
        } else {
            error = `HTTP ${status}`;
        }
    } catch (e) {
        error = e.message;
    }

    const duration = Date.now() - startTime;

    return { duration, status, success, error };
}

async function runConcurrencyTest(endpoint, concurrency, count) {
    const promises = [];
    for (let i = 0; i < count; i++) {
        promises.push(apiRequest(endpoint, concurrency));
    }
    return Promise.all(promises);
}

function analyzeResults(results) {
    const durations = results.map(r => r.duration);
    const successes = results.filter(r => r.success);
    const failures = results.filter(r => !r.success);

    const totalTime = Math.max(...durations) - Math.min(...durations);
    const avgDuration = durations.reduce((a, b) => a + b, 0) / durations.length;

    return {
        total: results.length,
        successes: successes.length,
        failures: failures.length,
        successRate: (successes.length / results.length * 100).toFixed(2) + '%',
        avgResponseTime: Math.round(avgDuration),
        minResponseTime: Math.min(...durations),
        maxResponseTime: Math.max(...durations),
        p50: percentile(durations, 50),
        p90: percentile(durations, 90),
        p99: percentile(durations, 99),
        throughput: Math.round(results.length / (totalTime / 1000) || 0),
        errors: [...new Set(failures.map(r => r.error))].filter(Boolean).slice(0, 3)
    };
}

async function getAuthToken() {
    console.log('Getting auth token...');
    try {
        const response = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ account: 'seeduser1', password: '123456' })
        });
        const data = await response.json();
        authToken = data.data?.token;
        console.log('Auth token obtained');
    } catch (e) {
        console.error('Failed to get auth token:', e.message);
    }
    console.log('');
}

async function warmUp() {
    console.log('Warming up APIs...');
    await Promise.all([
        apiRequest(TEST_ENDPOINTS[0], 1),
        apiRequest(TEST_ENDPOINTS[1], 1),
    ]);
    console.log('Warm-up complete\n');
}

async function runTests() {
    console.log('='.repeat(70));
    console.log('         CONCURRENT API PERFORMANCE TEST REPORT');
    console.log('='.repeat(70));
    console.log('');

    const report = {
        timestamp: new Date().toISOString(),
        environment: 'localhost:8080',
        testType: 'Concurrent Load Testing',
        concurrencyLevels: CONCURRENCY_LEVELS,
        requestsPerLevel: REQUESTS_PER_LEVEL,
        endpoints: TEST_ENDPOINTS.length,
        results: {}
    };

    await getAuthToken();
    await warmUp();

    for (const level of CONCURRENCY_LEVELS) {
        console.log(`\n${'─'.repeat(70)}`);
        console.log(`  CONCURRENCY LEVEL: ${level} simultaneous requests`);
        console.log(`${'─'.repeat(70)}`);

        report.results[`level_${level}`] = {
            concurrency: level,
            endpoints: {}
        };

        for (const endpoint of TEST_ENDPOINTS) {
            const label = `${endpoint.method} ${endpoint.path}`;
            console.log(`\n  Testing: ${endpoint.name}...`);
            console.log(`  Running ${REQUESTS_PER_LEVEL} requests with concurrency ${level}...`);

            const startTime = Date.now();
            const results = await runConcurrencyTest(endpoint, level, REQUESTS_PER_LEVEL);
            const totalTime = (Date.now() - startTime) / 1000;

            const analysis = analyzeResults(results);

            report.results[`level_${level}`].endpoints[endpoint.name] = {
                ...analysis,
                totalTime: totalTime.toFixed(2) + 's'
            };

            console.log(`  Results:`);
            console.log(`    - Success Rate: ${analysis.successRate}`);
            console.log(`    - Avg Response: ${formatMs(analysis.avgResponseTime)}`);
            console.log(`    - Min/Max: ${formatMs(analysis.minResponseTime)} / ${formatMs(analysis.maxResponseTime)}`);
            console.log(`    - P50/P90/P99: ${formatMs(analysis.p50)} / ${formatMs(analysis.p90)} / ${formatMs(analysis.p99)}`);
            console.log(`    - Throughput: ${analysis.throughput} req/s`);

            if (analysis.errors.length > 0) {
                console.log(`    - Errors: ${analysis.errors.join(', ')}`);
            }

            await sleep(500);
        }
    }

    // Generate summary
    console.log('\n\n' + '='.repeat(70));
    console.log('  SUMMARY BY CONCURRENCY LEVEL');
    console.log('='.repeat(70));

    for (const level of CONCURRENCY_LEVELS) {
        const levelData = report.results[`level_${level}`];
        const endpointResults = Object.values(levelData.endpoints);

        const avgSuccessRate = (endpointResults.reduce((a, r) => a + parseFloat(r.successRate), 0) / endpointResults.length).toFixed(2);
        const avgResponseTime = Math.round(endpointResults.reduce((a, r) => a + r.avgResponseTime, 0) / endpointResults.length);
        const maxThroughput = Math.max(...endpointResults.map(r => r.throughput));
        const totalFailures = endpointResults.reduce((a, r) => a + r.failures, 0);

        console.log(`\n  Level ${level}:`);
        console.log(`    Avg Success Rate: ${avgSuccessRate}%`);
        console.log(`    Avg Response Time: ${formatMs(avgResponseTime)}`);
        console.log(`    Max Throughput: ${maxThroughput} req/s`);
        console.log(`    Total Failures: ${totalFailures}`);

        report.results[`level_${level}`].summary = {
            avgSuccessRate,
            avgResponseTime,
            maxThroughput,
            totalFailures
        };
    }

    // Find best/worst performing endpoints
    console.log('\n' + '='.repeat(70));
    console.log('  ENDPOINT RANKINGS (by P99 at concurrency=100)');
    console.log('='.repeat(70));

    const level100Results = Object.entries(report.results.level_100.endpoints)
        .sort((a, b) => a[1].p99 - b[1].p99);

    console.log('\n  Fastest (lowest P99):');
    level100Results.slice(0, 5).forEach(([name, data], i) => {
        console.log(`    ${i + 1}. ${name}: P99=${formatMs(data.p99)}, success=${data.successRate}`);
    });

    console.log('\n  Slowest (highest P99):');
    level100Results.slice(-5).reverse().forEach(([name, data], i) => {
        console.log(`    ${i + 1}. ${name}: P99=${formatMs(data.p99)}, success=${data.successRate}`);
    });

    // Overall assessment
    console.log('\n' + '='.repeat(70));
    console.log('  OVERALL ASSESSMENT');
    console.log('='.repeat(70));

    const allSuccessRates = Object.values(report.results).flatMap(level =>
        Object.values(level.endpoints).map(e => parseFloat(e.successRate))
    );
    const allP99s = Object.values(report.results).flatMap(level =>
        Object.values(level.endpoints).map(e => e.p99)
    );

    const avgSuccessRate = (allSuccessRates.reduce((a, b) => a + b, 0) / allSuccessRates.length).toFixed(2);
    const avgP99 = Math.round(allP99s.reduce((a, b) => a + b, 0) / allP99s.length);
    const maxP99 = Math.max(...allP99s);

    console.log(`\n  Overall Success Rate: ${avgSuccessRate}%`);
    console.log(`  Overall Avg P99: ${formatMs(avgP99)}`);
    console.log(`  Worst P99: ${formatMs(maxP99)}`);

    // Performance rating
    let rating = 'EXCELLENT';
    if (avgSuccessRate < 99 || maxP99 > 5000) rating = 'GOOD';
    if (avgSuccessRate < 95 || maxP99 > 10000) rating = 'FAIR';
    if (avgSuccessRate < 90 || maxP99 > 30000) rating = 'POOR';

    console.log(`  Performance Rating: ${rating}`);

    console.log('\n' + '='.repeat(70));
    console.log(`  Test completed at: ${new Date().toLocaleString()}`);
    console.log('='.repeat(70));

    return report;
}

// Run tests
runTests().catch(console.error);
