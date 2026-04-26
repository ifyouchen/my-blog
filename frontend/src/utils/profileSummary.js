const formatValue = (value, loading) => (loading ? '--' : value || 0);

export const buildProfileSummaryStats = (profile = {}, options = {}) => {
    const {
        includeSocial = false,
        loading = false
    } = options;

    const stats = [
        {
            key: 'articles',
            label: '文章',
            value: formatValue(profile.articleCount, loading)
        },
        {
            key: 'views',
            label: '阅读',
            value: formatValue(profile.totalViewCount, loading)
        },
        {
            key: 'likes',
            label: '获赞',
            value: formatValue(profile.totalLikeCount, loading)
        }
    ];

    if (includeSocial) {
        stats.push(
            {
                key: 'followers',
                label: '粉丝',
                value: formatValue(profile.followerCount, loading)
            },
            {
                key: 'following',
                label: '关注',
                value: formatValue(profile.followingCount, loading)
            }
        );
    }

    return stats;
};
