<div>Test 01 as executed ${url.match}</div>
<div>Test 01 args: <#list args?keys as key>${key}=${args[key]} </#list></div>
<div>Test 01 component url model: ${url.context} and ${url.full} and ${url.args}</div>
<div>Test 01 page url model:</div>
<ul>
<li>page.url.context: ${page.url.context}</li>
<li>page.url.servletContext: ${page.url.servletContext}</li>
<li>page.url.uri: ${page.url.uri}</li>
<li>page.url.url: ${page.url.url}</li>
<li>page.url.args: ${page.url.args}</li>
<ul>