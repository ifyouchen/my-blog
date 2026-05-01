export const ARTICLE_SORT_LATEST = 'latest';
export const ARTICLE_SORT_HOT = 'hot';
export const ARTICLE_SORT_FEATURED = 'featured';

export const ARTICLE_SORT_ITEMS = [
    { label: '最新', value: ARTICLE_SORT_LATEST },
    { label: '最热', value: ARTICLE_SORT_HOT },
    { label: '高赞', value: ARTICLE_SORT_FEATURED }
];

export const normalizeArticleSort = (value) => {
    if (value === ARTICLE_SORT_HOT || value === ARTICLE_SORT_FEATURED) {
        return value;
    }
    return ARTICLE_SORT_LATEST;
};

export const isDefaultArticleSort = (value) => normalizeArticleSort(value) === ARTICLE_SORT_LATEST;
