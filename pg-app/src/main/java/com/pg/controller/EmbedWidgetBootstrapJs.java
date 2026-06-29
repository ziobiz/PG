package com.pg.controller;

/**
 * embed-checkout / embed-pay 부트스트랩 JS 체인 — icopay-checkout-3ds → embed-widget-common → lang → widget.
 */
final class EmbedWidgetBootstrapJs {

    private EmbedWidgetBootstrapJs() {
    }

    static String build(String cfgGlobalName, String compIdJson, String widgetScript, int widgetVer) {
        return "(function(){"
                + "var cur=document.currentScript;"
                + "if(!cur||!cur.src){console.error('[ICOPAY] embed bootstrap: no currentScript');return;}"
                + "var u=new URL(cur.src);"
                + "var origin=u.origin;"
                + "window." + cfgGlobalName + "={compId:" + compIdJson + ",origin:origin,script:cur};"
                + "function ld(src,cb){var s=document.createElement('script');s.src=src;s.charset='utf-8';"
                + "s.onload=cb||function(){};document.head.appendChild(s);}"
                + "ld(origin+'/js/icopay-checkout-3ds.js?v=1',function(){"
                + "ld(origin+'/js/icopay-embed-widget-common.js?v=1',function(){"
                + "ld(origin+'/js/icopay-checkout-lang.js?v=2',function(){"
                + "ld(origin+'/js/" + widgetScript + "?v=" + widgetVer + "');"
                + "});});});"
                + "})();";
    }
}
