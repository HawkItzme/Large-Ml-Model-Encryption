# === PREREQUISITES ===
# 1. Install pycryptodome library:
#    pip install pycryptodome
#
# 2. Ensure Python 3 is installed on your Windows machine (preferably 3.8+).
#
# 3. Prepare the following paths:
#    - input_model: Path to the original .tflite (or other FlatBuffer) model file you want to encrypt
#    - output_model: Path where the encrypted model will be saved
#    - output_key: Path where the AES key and metadata (JSON file) will be saved
#
# 4. Ensure you have enough disk space (since a copy of the model will be created).

import os
import json
import base64
from Crypto.Cipher import AES
from Crypto.Random import get_random_bytes

def encrypt_entire_flatbuffer(input_model_path, encrypted_model_path, key_output_path, segment_size=64 * 1024):
    # Read file as bytearray to modify in-place
    with open(input_model_path, 'rb') as f:
        model_data = bytearray(f.read())

    aes_key = get_random_bytes(32)
    encrypted_segments = []

    file_size = len(model_data)
    print(f"🔍 Model size: {file_size / (1024 * 1024):.2f} MB")

    for offset in range(0, file_size, segment_size):
        chunk = model_data[offset:offset + segment_size]
        nonce = get_random_bytes(12)
        cipher = AES.new(aes_key, AES.MODE_GCM, nonce=nonce)
        ciphertext, tag = cipher.encrypt_and_digest(chunk)

        # Replace in-place
        model_data[offset:offset + len(chunk)] = ciphertext

        encrypted_segments.append({
            "offset": offset,
            "length": len(chunk),
            "nonce": base64.b64encode(nonce).decode(),
            "tag": base64.b64encode(tag).decode()
        })

    # Save encrypted model
    with open(encrypted_model_path, 'wb') as f:
        f.write(model_data)

    # Save AES key + encryption metadata
    metadata = {
        "key": base64.b64encode(aes_key).decode(),
        "segments": encrypted_segments
    }

    with open(key_output_path, 'w') as f:
        json.dump(metadata, f, indent=2)

    print(f"✅ Encrypted {len(encrypted_segments)} segments of FlatBuffer")
    print(f"📄 Encrypted model saved to: {encrypted_model_path}")
    print(f"🔐 AES key + metadata saved to: {key_output_path}")

# === CONFIGURE PATHS ===
input_model = r'... ' # Original model path
output_model = r'...' # Encrypted model's output path
output_key = r'...'  # Metadata JSON output path

encrypt_entire_flatbuffer(input_model, output_model, output_key)
