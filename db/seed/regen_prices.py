import random

random.seed(42)

PRICE_RANGES = {
    "CLOTHING": (1500, 12900),
    "ELECTRONICS": (1200, 3890),
    "SPORTS": (990, 2590),
    "FURNITURE": (1900, 4590),
    "BEVERAGE": (490, 4200),
    "BEAUTY": (890, 6800),
    "FOOD": (590, 8900),
    "PET": (490, 1190),
    "OFFICE_SUPPLIES": (190, 3900),
    "BOOKS": (980, 3200),
    "OTHER": (590, 2590),
}

lines = []
with open("/private/tmp/claude-501/-Users-hyungtak-Desktop-plus-spring-spring-plus/d7da6e8c-e584-471c-a6ca-8d9bd4186339/scratchpad/current_products.tsv", encoding="utf-8") as f:
    for line in f:
        pid, name, category, status, discount_rate = line.rstrip("\n").split("\t")
        discount_rate = int(discount_rate)
        lo, hi = PRICE_RANGES[category]
        new_price = round(random.randint(lo, hi) / 100) * 100

        if status == "ON_EVENT" and discount_rate > 0:
            new_event_price = round(new_price * (100 - discount_rate) / 100 / 100) * 100
            lines.append(f"UPDATE products SET price = {new_price}, event_price = {new_event_price} WHERE id = {pid};")
        else:
            lines.append(f"UPDATE products SET price = {new_price} WHERE id = {pid};")

print(f"-- 상품 {len(lines)}개 가격을 새 범위로 재조정 (id/이미지/재고/이벤트연결 등은 그대로 유지)")
for l in lines:
    print(l)
