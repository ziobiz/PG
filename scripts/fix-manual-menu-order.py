# -*- coding: utf-8 -*-
"""Reorder HQ policy hub sections in operator manuals (access → AI → platform)."""
import re
import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[1] / "docs"


def reorder_super_manual(fp: pathlib.Path, lang: str) -> None:
    text = fp.read_text(encoding="utf-8")
    if lang == "ko":
        m_plat = re.search(
            r"(    <!-- ══ 7\. 플랫폼 ══ -->.*?)(    <!-- ══ 8\. 접근·권한 ══ -->)", text, re.S
        )
        m_access = re.search(
            r"(    <!-- ══ 8\. 접근·권한 ══ -->.*?)(    <!-- ══ 9\. AI·챗봇 ══ -->)", text, re.S
        )
        m_ai = re.search(
            r"(    <!-- ══ 9\. AI·챗봇 ══ -->.*?)(    <!-- ══ 10\. PG사 연동 ══ -->)", text, re.S
        )
        if not (m_plat and m_access and m_ai):
            print("skip sections", fp.name)
            return
        plat, access, ai = m_plat.group(1), m_access.group(1), m_ai.group(1)

        def renum_access(s: str) -> str:
            s = s.replace("<!-- ══ 8. 접근·권한 ══ -->", "<!-- ══ 7. 접근·권한 ══ -->")
            s = re.sub(r'(id="access-perm">)8\.', r"\g<1>7.", s)
            return s.replace("8-1.", "7-1.").replace("8-2.", "7-2.").replace("8-3.", "7-3.")

        def renum_ai(s: str) -> str:
            s = s.replace("<!-- ══ 9. AI·챗봇 ══ -->", "<!-- ══ 8. AI·챗봇 ══ -->")
            s = re.sub(r'(id="ai-chatbot">)9\.', r"\g<1>8.", s)
            return s

        def renum_plat(s: str) -> str:
            s = s.replace("<!-- ══ 7. 플랫폼 ══ -->", "<!-- ══ 9. 플랫폼 ══ -->")
            s = re.sub(r'(id="platform">)7\.', r"\g<1>9.", s)
            return (
                s.replace("7-1.", "9-1.")
                .replace("7-2.", "9-2.")
                .replace("7-3.", "9-3.")
                .replace("7-4.", "9-4.")
            )

        new_mid = renum_access(access) + renum_ai(ai) + renum_plat(plat)
        text = text[: m_plat.start()] + new_mid + text[m_ai.end(1) :]
        text = text.replace(
            '<li><a href="#org-screen">본사정책 — 조직·화면</a></li>\n'
            '        <li><a href="#platform">본사정책 — 플랫폼</a></li>\n'
            '        <li><a href="#access-perm">본사정책 — 접근·권한</a></li>\n'
            '        <li><a href="#ai-chatbot">본사정책 — AI·챗봇</a></li>',
            '<li><a href="#org-screen">본사정책 — 조직·화면</a></li>\n'
            '        <li><a href="#access-perm">본사정책 — 접근·권한</a></li>\n'
            '        <li><a href="#ai-chatbot">본사정책 — AI·챗봇</a></li>\n'
            '        <li><a href="#platform">본사정책 — 플랫폼</a></li>',
        )
        old_rows = (
            '        <tr><td>플랫폼 허브</td><td>전산·동기화, 도메인·SSL, 서버, <strong>업데이트 내용</strong></td>'
            '<td class="icon">✅</td><td class="icon">❌</td><td class="icon">❌</td></tr>\n'
            '        <tr><td>접근·권한 허브</td><td>본사 권한, 사용자, 업체 접근</td>'
            '<td class="icon">✅</td><td class="icon">❌</td><td class="icon">❌</td></tr>\n'
            '        <tr><td>AI·챗봇 (단독)</td><td>AI 모델 설정, 챗봇 전역 설정</td>'
            '<td class="icon">✅</td><td class="icon">❌</td><td class="icon">❌</td></tr>'
        )
        new_rows = (
            '        <tr><td>접근·권한 허브</td><td>본사 권한, 사용자, 업체 접근</td>'
            '<td class="icon">✅</td><td class="icon">❌</td><td class="icon">❌</td></tr>\n'
            '        <tr><td>AI·챗봇 (단독)</td><td>AI 모델 설정, 챗봇 전역 설정</td>'
            '<td class="icon">✅</td><td class="icon">❌</td><td class="icon">❌</td></tr>\n'
            '        <tr><td>플랫폼 허브</td><td>전산·동기화, 도메인·SSL, 서버, <strong>업데이트 내용</strong></td>'
            '<td class="icon">✅</td><td class="icon">❌</td><td class="icon">❌</td></tr>'
        )
        text = text.replace(old_rows, new_rows)
    elif lang == "en":
        m_plat = re.search(
            r"(    <!-- 7\. Platform -->.*?)(    <!-- 8\. Access & Permissions -->)", text, re.S
        )
        m_access = re.search(
            r"(    <!-- 8\. Access & Permissions -->.*?)(    <!-- 9\. AI & Chatbot -->)", text, re.S
        )
        m_ai = re.search(
            r"(    <!-- 9\. AI & Chatbot -->.*?)(    <!-- 10\. PG Integration -->)", text, re.S
        )
        if not (m_plat and m_access and m_ai):
            print("skip sections", fp.name)
            return
        plat, access, ai = m_plat.group(1), m_access.group(1), m_ai.group(1)

        def renum_access(s: str) -> str:
            s = s.replace("<!-- 8. Access & Permissions -->", "<!-- 7. Access & Permissions -->")
            s = re.sub(r'(id="access-perm">)8\.', r"\g<1>7.", s)
            return s.replace("8-1.", "7-1.").replace("8-2.", "7-2.").replace("8-3.", "7-3.")

        def renum_ai(s: str) -> str:
            s = s.replace("<!-- 9. AI & Chatbot -->", "<!-- 8. AI & Chatbot -->")
            s = re.sub(r'(id="ai-chatbot">)9\.', r"\g<1>8.", s)
            return s

        def renum_plat(s: str) -> str:
            s = s.replace("<!-- 7. Platform -->", "<!-- 9. Platform -->")
            s = re.sub(r'(id="platform">)7\.', r"\g<1>9.", s)
            return s.replace("7-1.", "9-1.").replace("7-2.", "9-2.").replace("7-3.", "9-3.")

        new_mid = renum_access(access) + renum_ai(ai) + renum_plat(plat)
        text = text[: m_plat.start()] + new_mid + text[m_ai.end(1) :]
        text = text.replace(
            '<li><a href="#org-screen">HQ Policy — Organization &amp; Screen</a></li>\n'
            '        <li><a href="#platform">HQ Policy — Platform</a></li>\n'
            '        <li><a href="#access-perm">HQ Policy — Access &amp; Permissions</a></li>\n'
            '        <li><a href="#ai-chatbot">HQ Policy — AI &amp; Chatbot</a></li>',
            '<li><a href="#org-screen">HQ Policy — Organization &amp; Screen</a></li>\n'
            '        <li><a href="#access-perm">HQ Policy — Access &amp; Permissions</a></li>\n'
            '        <li><a href="#ai-chatbot">HQ Policy — AI &amp; Chatbot</a></li>\n'
            '        <li><a href="#platform">HQ Policy — Platform</a></li>',
        )
        old_rows = (
            '        <tr><td>Platform Hub</td><td>Systems &amp; Sync, Domain &amp; SSL, Server</td>'
            '<td class="icon">✅</td><td class="icon">❌</td><td class="icon">❌</td></tr>\n'
            '        <tr><td>Access &amp; Permissions Hub</td><td>HQ Permissions, Users, Merchant Access</td>'
            '<td class="icon">✅</td><td class="icon">❌</td><td class="icon">❌</td></tr>\n'
            '        <tr><td>AI &amp; Chatbot (standalone)</td><td>AI Model Settings, Global Chatbot Config</td>'
            '<td class="icon">✅</td><td class="icon">❌</td><td class="icon">❌</td></tr>'
        )
        new_rows = (
            '        <tr><td>Access &amp; Permissions Hub</td><td>HQ Permissions, Users, Merchant Access</td>'
            '<td class="icon">✅</td><td class="icon">❌</td><td class="icon">❌</td></tr>\n'
            '        <tr><td>AI &amp; Chatbot (standalone)</td><td>AI Model Settings, Global Chatbot Config</td>'
            '<td class="icon">✅</td><td class="icon">❌</td><td class="icon">❌</td></tr>\n'
            '        <tr><td>Platform Hub</td><td>Systems &amp; Sync, Domain &amp; SSL, Server, <strong>Release notes</strong></td>'
            '<td class="icon">✅</td><td class="icon">❌</td><td class="icon">❌</td></tr>'
        )
        text = text.replace(old_rows, new_rows)
    else:
        return
    fp.write_text(text, encoding="utf-8")
    print("reordered", fp.name)


def patch_hq_en() -> None:
    fp = ROOT / "icopay-operator-manual-hq-en.html"
    t = fp.read_text(encoding="utf-8")
    t = t.replace(
        "Organization & Screens, Platform, Access & Permissions, AI & Chatbot",
        "Organization & Screens, Access & Permissions, AI & Chatbot, Platform",
    )
    t = t.replace(
        "Organization & Screens / Platform / Access & Permissions / AI & Chatbot",
        "Organization & Screens / Access & Permissions / AI & Chatbot / Platform",
    )
    fp.write_text(t, encoding="utf-8")
    print("updated hq-en")


if __name__ == "__main__":
    reorder_super_manual(ROOT / "icopay-operator-manual.html", "ko")
    reorder_super_manual(ROOT / "icopay-operator-manual-en.html", "en")
    patch_hq_en()
