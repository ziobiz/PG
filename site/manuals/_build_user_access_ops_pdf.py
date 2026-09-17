# -*- coding: utf-8 -*-
"""Generate user-access-ops PDFs (user management + link preview) × 5 langs."""
from __future__ import annotations

from pathlib import Path

import fitz

ROOT = Path(__file__).resolve().parent
OUT = ROOT / "pdf" / "user-access-ops"
VERSION = "2.81"
DATE = "2026-09-17"

FONT_FILES = [
    Path(r"C:\Windows\Fonts\malgun.ttf"),
    Path(r"C:\Windows\Fonts\NotoSansKR-VF.ttf"),
    Path(r"C:\Windows\Fonts\msgothic.ttc"),
    Path(r"C:\Windows\Fonts\msyh.ttc"),
]


def fontfile() -> str:
    for p in FONT_FILES:
        if p.exists():
            return str(p)
    raise SystemExit("CJK font not found")


CONTENT = {
    "ko": {
        "title": "사용자관리 · 링크 미리보기 안내",
        "sub": "조직·설정권한·VIEW SETTING · 관리자 URL Open Graph",
        "sections": [
            (
                "1. 사용자관리",
                "메뉴: 사용자관리 → 사용자관리\n"
                "• 조직: 연락처와 권한그룹 사이. 총본사·본사·총판·지사·대리점·영업점·가맹점\n"
                "• 권한그룹*: 수정용(셀렉트). 대표·SUPERVISOR는 조회 전용\n"
                "• 설정권한: 현재 적용 표시만(수정 아님)\n"
                "• VIEW SETTING: 열 표시·순서. 고정 No./업체코드/업체명/사용자ID\n"
                "• 시스템 USER/ADMIN 문자열은 목록에 사용하지 않음",
            ),
            (
                "2. 설정권한 단축 표기",
                "감독담당→감독 · 관리담당→관리 · 운영담당→운영 · 정산담당→정산\n"
                "기술담당→기술 · 대표→대표 · 업체사용자→일반 · CHATBOT→챗봇\n"
                "DB 권한그룹명은 유지하고 화면 표시만 단축합니다.",
            ),
            (
                "3. 링크 미리보기(Open Graph)",
                "관리자 URL을 LINE·WhatsApp 등에 공유할 때 카드(제목·설명·이미지).\n"
                "설정: 조직 브랜드 → 「링크 미리보기 (메신저)」(제목·설명 5개국어)\n"
                "총본사: 직접 입력 · 본사/총판: 본사설정 따름 또는 직접 입력\n"
                "메신저 캐시로 예전 카드가 남을 수 있음. 서버 첫 HTML의 OG 메타가 기준.",
            ),
            (
                "4. 관련 문서",
                "관리자 메뉴 운영 가이드 · 본사설정_계정_OTP_사용자관리.md\n"
                "운영_링크미리보기_OpenGraph.md · 본사정책 → 플랫폼 → 업데이트 내용",
            ),
        ],
    },
    "en": {
        "title": "User Management · Link Preview Guide",
        "sub": "Org level, applied grant, VIEW SETTING · Admin URL Open Graph",
        "sections": [
            (
                "1. User management",
                "Menu: Users → User management\n"
                "• Org level between Contact and Permission group\n"
                "• Permission group*: editable · Applied grant: display only\n"
                "• VIEW SETTING · fixed: No./company code/name/user ID\n"
                "• System USER/ADMIN is not shown as the grant column",
            ),
            (
                "2. Short labels",
                "Supervisor · Admin · Ops · Settlement · Tech · Primary · General · Chatbot",
            ),
            (
                "3. Link preview (OG)",
                "Brand → Link preview (messenger), 5 languages.\n"
                "HQ: custom · Regional/Distributor: Follow HQ or custom.\n"
                "Messenger cache may keep old cards.",
            ),
            ("4. Related", "Admin menu ops guide · Platform → Release notes"),
        ],
    },
    "ja": {
        "title": "ユーザー管理 · リンクプレビュー案内",
        "sub": "組織・設定権限・VIEW SETTING · 管理URL Open Graph",
        "sections": [
            (
                "1. ユーザー管理",
                "メニュー: ユーザー管理\n"
                "• 組織列 · 権限グループ*(編集) · 設定権限(表示)\n"
                "• VIEW SETTING · システムUSERは使わない",
            ),
            (
                "2. 短縮表示",
                "監督・管理・運用・精算・技術・代表・一般・チャットボット",
            ),
            (
                "3. リンクプレビュー",
                "ブランド → リンクプレビュー（5言語）。総本部は直接入力、本社/総販は本社に従う/直接入力。",
            ),
            ("4. 関連", "管理者メニュー運用ガイド · 更新内容"),
        ],
    },
    "zh": {
        "title": "用户管理 · 链接预览说明",
        "sub": "组织、已设权限、VIEW SETTING · 管理端 URL Open Graph",
        "sections": [
            (
                "1. 用户管理",
                "菜单：用户管理\n"
                "• 组织列 · 权限组*（编辑）· 已设权限（仅显示）\n"
                "• VIEW SETTING · 不使用系统 USER",
            ),
            (
                "2. 简称",
                "督导、管理、运营、结算、技术、主账号、普通、机器人",
            ),
            (
                "3. 链接预览",
                "品牌 → 链接预览（五语）。总部自行输入；区域/总代可遵循总部或自行输入。",
            ),
            ("4. 相关", "管理员菜单运营指南 · 更新内容"),
        ],
    },
    "th": {
        "title": "จัดการผู้ใช้ · ตัวอย่างลิงก์",
        "sub": "องค์กร สิทธิ์ที่ตั้ง VIEW SETTING · Open Graph URL ผู้ดูแล",
        "sections": [
            (
                "1. จัดการผู้ใช้",
                "เมนู: จัดการผู้ใช้\n"
                "• คอลัมน์องค์กร · กลุ่มสิทธิ์* (แก้) · สิทธิ์ที่ตั้ง (แสดง)\n"
                "• VIEW SETTING · ไม่ใช้ USER ของระบบ",
            ),
            (
                "2. ป้ายย่อ",
                "ผู้กำกับ บริหาร ปฏิบัติการ ชำระบัญชี เทคนิค บัญชีหลัก ทั่วไป แชทบอท",
            ),
            (
                "3. ตัวอย่างลิงก์",
                "แบรนด์ → ตัวอย่างลิงก์ (5 ภาษา) HQ กรอกเอง · ภูมิภาค/ตัวแทน ตาม HQ หรือกรอกเอง",
            ),
            ("4. ที่เกี่ยวข้อง", "คู่มือเมนูผู้ดูแล · ประวัติอัปเดต"),
        ],
    },
}


def build(lang: str, font: str) -> None:
    c = CONTENT[lang]
    doc = fitz.open()
    page = doc.new_page(width=595, height=842)
    y = 48

    def put(text: str, size: float, color: tuple[float, float, float], x: float = 40) -> None:
        nonlocal y, page
        if y > 800:
            page = doc.new_page(width=595, height=842)
            y = 48
        page.insert_text((x, y), text, fontsize=size, fontfile=font, color=color)
        y += size + 6

    put(f"ICOPAY  V{VERSION}  ·  {DATE}", 9, (0.35, 0.35, 0.4))
    y += 8
    put(c["title"], 15, (0.1, 0.15, 0.25))
    put(c["sub"], 10, (0.3, 0.35, 0.4))
    y += 10
    for h, body in c["sections"]:
        put(h, 12, (0.12, 0.25, 0.45))
        for line in body.split("\n"):
            put(line[:100], 9, (0.15, 0.15, 0.18), x=48)
        y += 8
    OUT.mkdir(parents=True, exist_ok=True)
    out = OUT / f"{lang}.pdf"
    doc.save(out)
    doc.close()
    print("wrote", out)


def main() -> None:
    font = fontfile()
    for lang in CONTENT:
        build(lang, font)
    logo_src = ROOT / "pdf" / "super-ops" / "logo.png"
    if logo_src.exists():
        import shutil

        shutil.copy2(logo_src, OUT / "logo.png")
        print("copied logo")


if __name__ == "__main__":
    main()
