import base64

# 기타(OTHER) 카테고리 20개 상품 — 추상 그라디언트 대신 상품을 알아볼 수 있는 심플 라인 아이콘으로 교체.
# 각 아이콘은 흰색 스트로크로 그린 픽토그램 + 배경은 브랜드 레드/블루 계열을 번갈아 사용.

ICON_STYLE = 'fill="none" stroke="#ffffff" stroke-width="7" stroke-linecap="round" stroke-linejoin="round"'

ITEMS = [
    ("3단 접이식 우산", "#FF3B30", f'''
        <path {ICON_STYLE} d="M45,110 A55,55 0 0,1 155,110" />
        <path {ICON_STYLE} d="M45,110 Q55,100 65,110 Q75,100 85,110 Q95,100 105,110 Q115,100 125,110 Q135,100 145,110 Q150,100 155,110" />
        <line {ICON_STYLE} x1="100" y1="60" x2="100" y2="150" />
        <path {ICON_STYLE} d="M100,150 Q100,165 115,165" />
    '''),
    ("자동 장우산", "#1C4FD6", f'''
        <path {ICON_STYLE} d="M35,95 A65,65 0 0,1 165,95" />
        <path {ICON_STYLE} d="M35,95 Q45,85 55,95 Q65,85 75,95 Q85,85 95,95 Q105,85 115,95 Q125,85 135,95 Q145,85 155,95 Q160,85 165,95" />
        <line {ICON_STYLE} x1="100" y1="55" x2="100" y2="170" />
        <path {ICON_STYLE} d="M100,170 Q100,180 112,180" />
    '''),
    ("캐리어 20인치", "#FF3B30", f'''
        <rect {ICON_STYLE} x="50" y="70" width="100" height="105" rx="14" />
        <path {ICON_STYLE} d="M78,70 L78,45 Q78,35 88,35 L112,35 Q122,35 122,45 L122,70" />
        <line {ICON_STYLE} x1="100" y1="85" x2="100" y2="160" />
        <circle cx="72" cy="185" r="8" fill="#ffffff" />
        <circle cx="128" cy="185" r="8" fill="#ffffff" />
    '''),
    ("데일리 백팩", "#1C4FD6", f'''
        <path {ICON_STYLE} d="M60,90 Q60,50 100,50 Q140,50 140,90 L140,165 Q140,178 127,178 L73,178 Q60,178 60,165 Z" />
        <path {ICON_STYLE} d="M75,90 L125,90 L125,130 L75,130 Z" />
        <path {ICON_STYLE} d="M65,95 Q50,100 50,120 L50,140" />
        <path {ICON_STYLE} d="M135,95 Q150,100 150,120 L150,140" />
    '''),
    ("크로스백", "#FF3B30", f'''
        <rect {ICON_STYLE} x="60" y="100" width="80" height="65" rx="12" />
        <path {ICON_STYLE} d="M60,115 L35,40" />
        <path {ICON_STYLE} d="M140,115 L165,40" />
        <path {ICON_STYLE} d="M85,100 Q100,80 115,100" />
    '''),
    ("편광 선글라스", "#1C4FD6", f'''
        <circle {ICON_STYLE} cx="65" cy="105" r="32" />
        <circle {ICON_STYLE} cx="135" cy="105" r="32" />
        <line {ICON_STYLE} x1="97" y1="100" x2="103" y2="100" />
        <path {ICON_STYLE} d="M33,100 L15,90" />
        <path {ICON_STYLE} d="M167,100 L185,90" />
    '''),
    ("손목시계", "#FF3B30", f'''
        <circle {ICON_STYLE} cx="100" cy="100" r="45" />
        <path {ICON_STYLE} d="M100,75 L100,100 L118,112" />
        <path {ICON_STYLE} d="M80,55 L80,30 L120,30 L120,55" />
        <path {ICON_STYLE} d="M80,145 L80,170 L120,170 L120,145" />
    '''),
    ("가죽 반지갑", "#1C4FD6", f'''
        <rect {ICON_STYLE} x="45" y="70" width="110" height="75" rx="10" />
        <line {ICON_STYLE} x1="100" y1="70" x2="100" y2="145" />
        <path {ICON_STYLE} d="M60,95 L90,95" />
        <path {ICON_STYLE} d="M60,115 L90,115" />
    '''),
    ("메모리폼 목베개", "#FF3B30", f'''
        <path {ICON_STYLE} d="M45,80 Q45,150 100,150 Q155,150 155,80" />
        <circle cx="45" cy="75" r="12" fill="#ffffff" />
        <circle cx="155" cy="75" r="12" fill="#ffffff" />
    '''),
    ("보온보냉 텀블러", "#1C4FD6", f'''
        <path {ICON_STYLE} d="M70,55 L130,55 L122,175 Q122,185 110,185 L90,185 Q78,185 78,175 Z" />
        <ellipse {ICON_STYLE} cx="100" cy="55" rx="30" ry="10" />
        <path {ICON_STYLE} d="M130,90 Q155,95 155,115 Q155,135 130,138" />
    '''),
    ("보조가방", "#FF3B30", f'''
        <rect {ICON_STYLE} x="55" y="85" width="90" height="80" rx="16" />
        <path {ICON_STYLE} d="M75,85 Q75,60 100,60 Q125,60 125,85" />
        <line {ICON_STYLE} x1="70" y1="115" x2="130" y2="115" />
    '''),
    ("차량용 방향제", "#1C4FD6", f'''
        <rect {ICON_STYLE} x="78" y="110" width="44" height="55" rx="8" />
        <path {ICON_STYLE} d="M85,110 L80,50" />
        <path {ICON_STYLE} d="M100,110 L100,42" />
        <path {ICON_STYLE} d="M115,110 L120,50" />
    '''),
    ("휴대용 미니 선풍기", "#FF3B30", f'''
        <circle {ICON_STYLE} cx="100" cy="90" r="48" />
        <path {ICON_STYLE} d="M100,90 Q100,55 125,55 Q135,70 118,88" />
        <path {ICON_STYLE} d="M100,90 Q135,90 135,115 Q118,125 100,105" />
        <path {ICON_STYLE} d="M100,90 Q65,90 65,115 Q82,125 100,105" />
        <circle cx="100" cy="90" r="7" fill="#ffffff" />
        <line {ICON_STYLE} x1="100" y1="138" x2="100" y2="175" />
    '''),
    ("무선 휴대용 조명", "#1C4FD6", f'''
        <circle {ICON_STYLE} cx="100" cy="90" r="40" />
        <path {ICON_STYLE} d="M85,128 L85,150 L115,150 L115,128" />
        <line {ICON_STYLE} x1="90" y1="165" x2="110" y2="165" />
        <line {ICON_STYLE} x1="100" y1="30" x2="100" y2="18" />
        <line {ICON_STYLE} x1="50" y1="90" x2="38" y2="90" />
        <line {ICON_STYLE} x1="150" y1="90" x2="162" y2="90" />
    '''),
    ("캠핑용 랜턴", "#FF3B30", f'''
        <path {ICON_STYLE} d="M60,60 Q60,45 100,45 Q140,45 140,60" />
        <rect {ICON_STYLE} x="65" y="60" width="70" height="100" rx="10" />
        <line {ICON_STYLE} x1="80" y1="70" x2="80" y2="150" />
        <line {ICON_STYLE} x1="120" y1="70" x2="120" y2="150" />
        <circle cx="100" cy="45" r="6" fill="#ffffff" />
    '''),
    ("접이식 쇼핑카트", "#1C4FD6", f'''
        <path {ICON_STYLE} d="M40,55 L60,55 L80,140 L155,140" />
        <path {ICON_STYLE} d="M75,90 L150,90" />
        <path {ICON_STYLE} d="M65,140 L150,140 L140,90" />
        <circle {ICON_STYLE} cx="95" cy="165" r="11" />
        <circle {ICON_STYLE} cx="140" cy="165" r="11" />
    '''),
    ("멀티탭 세트", "#FF3B30", f'''
        <rect {ICON_STYLE} x="35" y="80" width="130" height="45" rx="20" />
        <circle cx="65" cy="102" r="7" fill="#ffffff" />
        <circle cx="100" cy="102" r="7" fill="#ffffff" />
        <circle cx="135" cy="102" r="7" fill="#ffffff" />
        <path {ICON_STYLE} d="M35,102 Q15,102 15,130" />
    '''),
    ("휴대폰 거치대", "#1C4FD6", f'''
        <path {ICON_STYLE} d="M50,165 L100,60 L150,165" />
        <rect {ICON_STYLE} x="80" y="75" width="40" height="75" rx="8" transform="rotate(0 100 112)" />
    '''),
    ("차량용 고속충전기", "#FF3B30", f'''
        <rect {ICON_STYLE} x="70" y="50" width="60" height="80" rx="12" />
        <line {ICON_STYLE} x1="85" y1="50" x2="85" y2="35" />
        <line {ICON_STYLE} x1="115" y1="50" x2="115" y2="35" />
        <path {ICON_STYLE} d="M95,130 Q95,160 120,160 L120,175" />
    '''),
    ("미니 가습기", "#1C4FD6", f'''
        <path {ICON_STYLE} d="M65,175 L65,120 Q65,80 100,80 Q135,80 135,120 L135,175 Z" />
        <path {ICON_STYLE} d="M85,65 Q80,55 85,45" />
        <path {ICON_STYLE} d="M100,60 Q95,48 100,35" />
        <path {ICON_STYLE} d="M115,65 Q110,55 115,45" />
    '''),
]


def make_svg(bg, icon_markup):
    svg = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200">
<rect width="200" height="200" fill="{bg}"/>
{icon_markup}
</svg>'''
    b64 = base64.b64encode(svg.encode("utf-8")).decode("ascii")
    return f"data:image/svg+xml;base64,{b64}"


def esc(s):
    return s.replace("'", "''")


print("-- 기타(OTHER) 카테고리 20개 상품의 대표이미지를 그라디언트 → 상품을 알아볼 수 있는 라인 아이콘으로 교체")
for name, bg, icon in ITEMS:
    image_url = make_svg(bg, icon)
    print(
        f"UPDATE products SET image_url = '{image_url}' "
        f"WHERE name = '{esc(name)}' AND category = 'OTHER';"
    )
