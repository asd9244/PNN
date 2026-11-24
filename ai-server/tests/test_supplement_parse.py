"""사진 인식 3단계(정리된 '이름: 함량' 줄 → 성분 목록) 테스트.

실행: ai-server 폴더에서 .venv\\Scripts\\python.exe -m unittest discover -s tests -t .
"""
import unittest

from app.api.v1.endpoints.supplement_recognition import parse_nutrients


def one(line):
    items = parse_nutrients(line)
    assert len(items) == 1, items
    return items[0]


class ParseNutrientsTest(unittest.TestCase):

    def test_아래_첨자_숫자는_보통_숫자로(self):
        self.assertEqual(one("Vitamin B₆: 6 mg")["name"], "Vitamin B6")
        self.assertEqual(one("Vitamin B₁₂: 100 mcg")["name"], "Vitamin B12")

    def test_이름_속_숫자를_함량으로_읽지_않음(self):
        item = one("Riboflavin (vitamin B2) (as riboflavin 5'-phosphate): 1.6 mg")
        self.assertEqual(item["name"], "Riboflavin (vitamin B2) (as riboflavin 5'-phosphate)")
        self.assertEqual((item["amount"], item["unit"]), (1.6, "mg"))

    def test_쉼표_있는_숫자는_한_덩어리로(self):
        self.assertEqual(one("Calcium: 1,000 mg")["amount"], 1000.0)

    def test_함량_뒤_설명은_무시하고_단위만(self):
        item = one("Vitamin A (as beta carotene): 1500 mcg RAE (5000 IU)")
        self.assertEqual((item["amount"], item["unit"]), (1500.0, "mcg"))

    def test_한글_성분과_마이크로그램(self):
        item = one("비타민D: 10 μg(100 %)")
        self.assertEqual((item["name"], item["amount"], item["unit"]), ("비타민D", 10.0, "μg"))

    def test_함량에_숫자가_없으면_뺌(self):
        self.assertEqual(parse_nutrients("Biotin: 표시 없음\n\n[원본 텍스트]"), [])

    def test_함량이_숫자로_시작하지_않는_첨가물_줄은_뺌(self):
        self.assertEqual(parse_nutrients("Magnesium Stearate: <2%\nFolate: (10 mcg L-methylfolate)"), [])

    def test_콜론이_없는_줄은_이름_뒤_첫_숫자를_함량으로(self):
        item = one("Zinc 15 mg")
        self.assertEqual((item["name"], item["amount"], item["unit"]), ("Zinc", 15.0, "mg"))

    def test_같은_이름은_하나만(self):
        self.assertEqual(len(parse_nutrients("Zinc: 15 mg\nZinc: 15 mg")), 1)


if __name__ == "__main__":
    unittest.main()
