import base64
from Crypto.Cipher import AES

# ===== 常量 =====
KEY_STORE_HASH = "nV7wsXr/HnExp2z8f9Nj3myC2II="
ivBytes = b"0000000000000000"  # 16个字符0

INPUT_FILE = "encode_data.txt"
OUTPUT_FILE = "decode_data.txt"


# ===== key生成 =====
def get_key_hash():
    key = KEY_STORE_HASH
    if len(key) < 32:
        padding = 32 - len(key)
        key += ivBytes[:padding].decode("utf-8")
    return key.encode("utf-8")


# ===== 去PKCS5/PKCS7 Padding =====
def remove_padding(data):
    if not data:
        return data
    pad = data[-1]
    # 增加安全性检查，防止 padding 越界崩溃
    if 1 <= pad <= 16:
        # 验证后面所有字节是否都等于 pad
        if data[-pad:] == bytes([pad]) * pad:
            return data[:-pad]
    return data


# ===== 解密 =====
def decode():
    key = get_key_hash()

    # 以文本模式读取，方便清洗掉不小心的换行或两侧空格
    with open(INPUT_FILE, "r", encoding="utf-8") as f:
        data_str = f.read().strip()

    # 核心防御：如果在复制密文时尾部带了系统的提示文本，进行截获清理
    if " " in data_str:
        # 密文本身是URL安全Base64，不应该包含空格。如果有空格，说明复制多了，只取第一段
        data_str = data_str.split()[0]

    data_bytes = data_str.encode("utf-8")

    try:
        # URL_SAFE Base64 规范化解码
        decoded = base64.urlsafe_b64decode(data_bytes)

        cipher = AES.new(key, AES.MODE_CBC, ivBytes)
        plain = cipher.decrypt(decoded)
        plain = remove_padding(plain)

        text = plain.decode("utf-8", errors="ignore")

        with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
            f.write(text)

        print(f"解密成功！结果已保存至: {OUTPUT_FILE}")

    except Exception as e:
        print(f"解密失败，请检查密文格式是否被破坏。错误原因: {e}")


if __name__ == "__main__":
    decode()