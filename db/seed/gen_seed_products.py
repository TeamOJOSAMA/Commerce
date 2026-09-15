import base64
import random

random.seed(42)

CATEGORY_LABELS = {
    "CLOTHING": "패션의류",
    "ELECTRONICS": "가전/디지털",
    "SPORTS": "스포츠/레저",
    "FURNITURE": "가구/인테리어",
    "BEVERAGE": "음료",
    "BEAUTY": "뷰티/화장품",
    "FOOD": "식품",
    "PET": "반려동물",
    "OFFICE_SUPPLIES": "문구/오피스",
    "BOOKS": "도서",
    "OTHER": "기타",
}

CATEGORIES = {
    "CLOTHING": {
        "items": [
            "크루넥 니트", "반팔 티셔츠", "데님 팬츠", "후드 집업", "트렌치 코트",
            "플리스 자켓", "와이드 슬랙스", "카고 팬츠", "니트 가디건", "롱 원피스",
            "미니 스커트", "블레이저 자켓", "스트라이프 셔츠", "조거 팬츠", "숏패딩",
            "무스탕 자켓", "레더 자켓", "니트 베스트", "체크 셔츠", "트레이닝 세트",
        ],
        "colors": ["#FF3B30", "#FF8A65"],
        "price": (1500, 12900),
    },
    "ELECTRONICS": {
        "items": [
            "무선 이어폰", "블루투스 스피커", "기계식 키보드", "무선 마우스", "27인치 모니터",
            "노트북 거치대", "보조배터리 20000mAh", "고속충전기 65W", "USB-C 허브", "웹캠",
            "게이밍 헤드셋", "스마트워치", "미니 프로젝터", "공기청정기", "로봇청소기",
            "전기포트", "에어프라이어", "무선청소기", "제습기", "가습기",
        ],
        "colors": ["#1C4FD6", "#6C8CFF"],
        "price": (1200, 3890),
    },
    "SPORTS": {
        "items": [
            "러닝화", "요가매트", "폼롤러", "덤벨 세트", "짐볼",
            "트레이닝 레깅스", "등산 배낭", "돔 텐트", "캠핑 체어", "자전거 헬멧",
            "스포츠 워치", "골프 장갑", "수영복", "클라이밍 홀드 세트", "축구공",
            "농구공", "배드민턴 라켓", "줄넘기", "폴딩 자전거", "캠핑 매트",
        ],
        "colors": ["#16A34A", "#86EFAC"],
        "price": (990, 2590),
    },
    "FURNITURE": {
        "items": [
            "원목 책상", "사무용 의자", "3인용 패브릭 소파", "침대 프레임", "메모리폼 매트리스",
            "원목 협탁", "붙박이형 옷장", "5단 책장", "LED 화장대", "스탠드 조명",
            "极세사 러그", "암막 커튼", "4인용 식탁 세트", "수납장", "전신 거울",
            "행거", "3단 서랍장", "데스크 매트", "파티션", "빈백 소파",
        ],
        "colors": ["#92400E", "#D2A679"],
        "price": (1900, 4590),
    },
    "BEVERAGE": {
        "items": [
            "콜드브루 원액", "드립백 커피 30입", "유기농 원두 1kg", "캡슐커피 세트", "홍차 티백 세트",
            "과일청 에이드베이스", "탄산수 24캔", "제로 콜라 20캔", "두유 세트", "프로틴 쉐이크",
            "비타민 워터", "보리차 티백", "녹차 티백", "이온음료 세트", "코코넛워터",
            "아이스티 파우더", "핫초코 믹스", "인스턴트 커피믹스", "라떼 베이스", "스포츠음료 세트",
        ],
        "colors": ["#0EA5E9", "#7DD3FC"],
        "price": (490, 4200),
    },
    "BEAUTY": {
        "items": [
            "수분크림", "무기자차 선크림", "클렌징폼", "약산성 토너", "히알루론산 에센스",
            "촉촉 립밤", "마스크팩 10매", "쿠션 파운데이션", "아이섀도 팔레트", "매트 립스틱",
            "헤어에센스", "바디로션", "핸드크림 세트", "미니 향수", "젤 아이라이너",
            "롱래스팅 마스카라", "클렌징 오일", "필링 패드", "스킨부스터 앰플", "자외선 차단 스틱",
        ],
        "colors": ["#EC4899", "#F9A8D4"],
        "price": (890, 6800),
    },
    "FOOD": {
        "items": [
            "유기농 그래놀라", "견과류 믹스", "저당 그릭요거트", "즉석 현미밥 세트", "프리미엄 조미김",
            "참치캔 세트", "라면 선물세트", "냉동 만두", "국물떡볶이 밀키트", "훈제오리",
            "한우 선물세트", "제철 과일 세트", "견과류 선물세트", "수제 잼", "엑스트라버진 올리브유",
            "발사믹 식초", "파스타면 세트", "파스타 소스 세트", "전통 한과 세트", "곡물 선물세트",
        ],
        "colors": ["#F59E0B", "#FCD34D"],
        "price": (590, 8900),
    },
    "PET": {
        "items": [
            "강아지 사료 3kg", "고양이 사료 3kg", "캣타워", "강아지 수제간식", "고양이 트릿",
            "반려동물 이동장", "펫 방석", "강아지 목줄", "고양이 자동화장실", "펫 저자극 샴푸",
            "강아지 겨울옷", "고양이 낚싯대 장난감", "배변패드 100매", "펫 자동급식기", "고양이 모래",
            "강아지 하네스", "펫 슬리커 브러시", "강아지 장난감 세트", "고양이 스크래처", "차량용 펫 카시트",
        ],
        "colors": ["#A855F7", "#D8B4FE"],
        "price": (490, 1190),
    },
    "OFFICE_SUPPLIES": {
        "items": [
            "젤펜 10자루 세트", "위클리 다이어리", "형광펜 6color 세트", "포스트잇 세트", "A4 복사용지 2500매",
            "클리어파일 20p", "스테이플러", "만년필", "마스킹테이프 세트", "무지노트 3권 세트",
            "자석 화이트보드", "연필 12자루 세트", "지우개 세트", "샤프심 리필", "3공 파일 바인더",
            "네임펜 세트", "수정테이프", "북마크 세트", "탁상용 캘린더", "데스크 정리함",
        ],
        "colors": ["#6B7280", "#D1D5DB"],
        "price": (190, 3900),
    },
    "BOOKS": {
        "items": [
            "자기계발 베스트셀러", "화제의 소설 신간", "경제경영 필독서", "에세이 모음집", "IT 개발 입문서",
            "어린이 그림책 세트", "요리 레시피북", "여행 에세이", "인문학 교양서", "시집",
            "영어회화 교재", "만화책 세트", "다이어리 플래너북", "심리학 입문서", "재테크 가이드북",
            "역사 교양서", "과학 교양서", "자서전", "수험서", "그래픽노블",
        ],
        "colors": ["#78350F", "#B45309"],
        "price": (980, 3200),
    },
    "OTHER": {
        "items": [
            "3단 접이식 우산", "자동 장우산", "캐리어 20인치", "데일리 백팩", "크로스백",
            "편광 선글라스", "손목시계", "가죽 반지갑", "메모리폼 목베개", "보온보냉 텀블러",
            "보조가방", "차량용 방향제", "휴대용 미니 선풍기", "무선 휴대용 조명", "캠핑용 랜턴",
            "접이식 쇼핑카트", "멀티탭 세트", "휴대폰 거치대", "차량용 고속충전기", "미니 가습기",
        ],
        "colors": ["#1C4FD6", "#FF3B30"],
        "price": (590, 2590),
    },
}


def make_svg(c1, c2, seed):
    angle = 20 + (seed * 37) % 100
    cx1 = 40 + (seed * 23) % 120
    cy1 = 40 + (seed * 51) % 120
    r1 = 55 + (seed * 7) % 40
    cx2 = 160 - (seed * 17) % 100
    cy2 = 150 - (seed * 29) % 100
    r2 = 35 + (seed * 11) % 30
    svg = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200">
<defs><linearGradient id="g" x1="0%" y1="0%" x2="100%" y2="100%" gradientTransform="rotate({angle} .5 .5)">
<stop offset="0%" stop-color="{c1}" stop-opacity="0.9"/>
<stop offset="100%" stop-color="{c2}" stop-opacity="0.85"/>
</linearGradient></defs>
<rect width="200" height="200" fill="#111111"/>
<circle cx="{cx1}" cy="{cy1}" r="{r1}" fill="url(#g)"/>
<circle cx="{cx2}" cy="{cy2}" r="{r2}" fill="url(#g)" opacity="0.7"/>
</svg>'''
    b64 = base64.b64encode(svg.encode("utf-8")).decode("ascii")
    return f"data:image/svg+xml;base64,{b64}"


def esc(s):
    return s.replace("'", "''")


rows = []
seed_counter = 0
for category, cfg in CATEGORIES.items():
    c1, c2 = cfg["colors"]
    lo, hi = cfg["price"]
    for i, item_name in enumerate(cfg["items"]):
        seed_counter += 1
        name = item_name
        description = f"{name} — 갈팡질팡이 엄선한 {CATEGORY_LABELS[category]} 카테고리 인기 상품입니다. 꼼꼼히 확인하고 골라보세요."
        price = round(random.randint(lo, hi) / 100) * 100
        stock = random.randint(5, 200)
        image_url = make_svg(c1, c2, seed_counter)

        is_event = random.random() < 0.25
        if is_event:
            discount_rate = random.choice([10, 15, 20, 25, 30, 35, 40])
            event_price = round(price * (100 - discount_rate) / 100 / 100) * 100
            status = "ON_EVENT"
        else:
            discount_rate = "NULL"
            event_price = "NULL"
            status = "ON_SALE"

        rows.append({
            "name": esc(name),
            "description": esc(description),
            "category": category,
            "price": price,
            "stock": stock,
            "status": status,
            "event_price": event_price,
            "discount_rate": discount_rate,
            "image_url": image_url,
        })

print(f"-- 갈팡질팡 카테고리별 시드 상품 {len(rows)}개 (카테고리당 20개)")
print("-- seller_id는 기존 유저 중 가장 먼저 만들어진 계정을 그대로 사용한다.")
print("SET @seed_seller_id = (SELECT id FROM users ORDER BY id ASC LIMIT 1);")
print()
print("-- 재실행 시 중복 삽입을 막기 위해 이전 시드 데이터를 먼저 정리한다.")
print("DELETE FROM products WHERE name IN (")
names = sorted({r["name"] for r in rows})
print(",\n".join(f"  '{n}'" for n in names))
print(");")
print()
print("INSERT INTO products (created_at, updated_at, seller_id, name, description, category, price, stock, status, view_count, event_price, discount_rate, image_url) VALUES")

value_lines = []
for r in rows:
    value_lines.append(
        f"(NOW(), NOW(), @seed_seller_id, '{r['name']}', '{r['description']}', "
        f"'{r['category']}', {r['price']}, {r['stock']}, '{r['status']}', 0, "
        f"{r['event_price']}, {r['discount_rate']}, '{r['image_url']}')"
    )

print(",\n".join(value_lines) + ";")
