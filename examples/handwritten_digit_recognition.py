"""最简单的手写数字识别：加载数据 → 训练模型 → 预测数字。

安装依赖：python -m pip install scikit-learn
运行示例：python examples/handwritten_digit_recognition.py

本示例识别数据集里的数字图片，不包含手写画板或自定义图片上传。
"""

from sklearn.datasets import load_digits
from sklearn.model_selection import train_test_split
from sklearn.neighbors import KNeighborsClassifier


def main():
    # 1. 加载自带的数据集，无需另外下载。
    # 每张图片是 8×8 像素，展开后有 64 个数值；标签是数字 0～9。
    digits = load_digits()

    # 2. 用 80% 的图片训练，剩下 20% 测试，避免拿训练图片检验效果。
    # random_state 固定随机结果；stratify 尽量保持各数字的比例一致。
    x_train, x_test, y_train, y_test = train_test_split(
        digits.data, digits.target,
        test_size=0.2, random_state=42, stratify=digits.target,
    )

    # 3. K近邻：找到最相似的 3 张训练图片，用它们的标签投票。
    model = KNeighborsClassifier(n_neighbors=3)
    model.fit(x_train, y_train)

    # 4. 在没有参加训练的测试图片上计算识别准确率。
    print(f"测试集准确率：{model.score(x_test, y_test):.2%}")

    # 5. 识别第一张测试图片。[0:1] 保留“图片列表”的二维形状。
    prediction = model.predict(x_test[0:1])[0]
    print(f"识别结果：{prediction}，真实数字：{y_test[0]}")

    # 用字符粗略显示这张图片：像素越亮，字符越明显。
    shades = " .:-=+*#%@"
    print("测试图片：")
    for row in x_test[0].reshape(8, 8):
        print("".join(shades[int(pixel * 9 / 16)] for pixel in row))


# 直接运行本文件时执行；被其他文件导入时不自动训练。
if __name__ == "__main__":
    main()
