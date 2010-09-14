<#if asset.relatedAssets['ws:relatedArticles']?? && (asset.relatedAssets['ws:relatedArticles']?size > 0)>
    <div class="services-box">
        <h3>${msg('related.articles.title')}</h3>
        <ul class="services-box-list">
            <#list asset.relatedAssets['ws:relatedArticles'] as related>      
                <li>
                    <a href="<@makeurl asset=related/>">${related.title!'no title'}</a>
                </li>
            </#list>
        </ul>
    </div>
</#if>