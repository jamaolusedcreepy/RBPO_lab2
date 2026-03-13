#!/bin/bash
# =============================================================================
# Генерация цепочки сертификатов для Support Ticket System
# Цепочка: Root CA → Intermediate CA → Server
#
# ВАЖНО: Замените STUDENT_ID на номер своего студенческого билета!
# ВАЖНО: Все сгенерированные файлы исключены из git (см. .gitignore)
# =============================================================================

set -e

# Отключаем конвертацию путей в Git Bash на Windows
# (иначе /C=RU/... превращается в C:/Program Files/Git/C=RU/...)
export MSYS_NO_PATHCONV=1

STUDENT_ID="${STUDENT_ID:-XXXXXXXX}"   # <-- подставьте свой номер или задайте env: export STUDENT_ID=12345678
KEYSTORE_PASSWORD="${KEYSTORE_PASSWORD:-changeit}"
VALIDITY_ROOT=3650        # 10 лет
VALIDITY_INTERMEDIATE=1825 # 5 лет
VALIDITY_SERVER=365        # 1 год

OUTPUT_DIR="certs"
mkdir -p "$OUTPUT_DIR"

echo "=== [1/5] Генерация Root CA (Student-${STUDENT_ID}-RootCA) ==="
openssl genrsa -out "$OUTPUT_DIR/sts-root-ca.key" 4096

openssl req -new -x509 \
  -days $VALIDITY_ROOT \
  -key "$OUTPUT_DIR/sts-root-ca.key" \
  -out "$OUTPUT_DIR/sts-root-ca.crt" \
  -subj "/C=RU/ST=Moscow/L=Moscow/O=SupportTicketSystem/OU=Student-${STUDENT_ID}/CN=STS-RootCA"

echo "=== [2/5] Генерация Intermediate CA (Student-${STUDENT_ID}-IntermediateCA) ==="
openssl genrsa -out "$OUTPUT_DIR/sts-intermediate-ca.key" 4096

openssl req -new \
  -key "$OUTPUT_DIR/sts-intermediate-ca.key" \
  -out "$OUTPUT_DIR/sts-intermediate-ca.csr" \
  -subj "/C=RU/ST=Moscow/L=Moscow/O=SupportTicketSystem/OU=Student-${STUDENT_ID}/CN=STS-IntermediateCA"

cat > "$OUTPUT_DIR/intermediate-ext.cnf" << EOF
basicConstraints=CA:TRUE,pathlen:0
keyUsage=critical,keyCertSign,cRLSign
subjectKeyIdentifier=hash
authorityKeyIdentifier=keyid,issuer
EOF

openssl x509 -req \
  -days $VALIDITY_INTERMEDIATE \
  -in "$OUTPUT_DIR/sts-intermediate-ca.csr" \
  -CA "$OUTPUT_DIR/sts-root-ca.crt" \
  -CAkey "$OUTPUT_DIR/sts-root-ca.key" \
  -CAcreateserial \
  -out "$OUTPUT_DIR/sts-intermediate-ca.crt" \
  -extfile "$OUTPUT_DIR/intermediate-ext.cnf"

echo "=== [3/5] Генерация серверного сертификата (Student-${STUDENT_ID}-Server) ==="
openssl genrsa -out "$OUTPUT_DIR/sts-server.key" 2048

openssl req -new \
  -key "$OUTPUT_DIR/sts-server.key" \
  -out "$OUTPUT_DIR/sts-server.csr" \
  -subj "/C=RU/ST=Moscow/L=Moscow/O=SupportTicketSystem/OU=Student-${STUDENT_ID}/CN=localhost"

cat > "$OUTPUT_DIR/server-ext.cnf" << EOF
basicConstraints=CA:FALSE
keyUsage=critical,digitalSignature,keyEncipherment
extendedKeyUsage=serverAuth
subjectAltName=DNS:localhost,IP:127.0.0.1
EOF

openssl x509 -req \
  -days $VALIDITY_SERVER \
  -in "$OUTPUT_DIR/sts-server.csr" \
  -CA "$OUTPUT_DIR/sts-intermediate-ca.crt" \
  -CAkey "$OUTPUT_DIR/sts-intermediate-ca.key" \
  -CAcreateserial \
  -out "$OUTPUT_DIR/sts-server.crt" \
  -extfile "$OUTPUT_DIR/server-ext.cnf"

echo "=== [4/5] Сборка цепочки и создание PKCS12 keystore ==="
cat "$OUTPUT_DIR/sts-server.crt" \
    "$OUTPUT_DIR/sts-intermediate-ca.crt" \
    "$OUTPUT_DIR/sts-root-ca.crt" > "$OUTPUT_DIR/sts-chain.crt"

openssl pkcs12 -export \
  -in "$OUTPUT_DIR/sts-server.crt" \
  -inkey "$OUTPUT_DIR/sts-server.key" \
  -certfile "$OUTPUT_DIR/sts-chain.crt" \
  -name server \
  -out src/main/resources/keystore.p12 \
  -passout "pass:${KEYSTORE_PASSWORD}"

echo "=== [5/5] Верификация цепочки ==="
openssl verify \
  -CAfile "$OUTPUT_DIR/sts-root-ca.crt" \
  -untrusted "$OUTPUT_DIR/sts-intermediate-ca.crt" \
  "$OUTPUT_DIR/sts-server.crt"

echo ""
echo "======================================================"
echo " Готово! Файлы созданы в директории: $OUTPUT_DIR/"
echo " Keystore: src/main/resources/keystore.p12"
echo "======================================================"
echo ""
echo "Запустить приложение с HTTPS:"
echo "  SSL_ENABLED=true SSL_KEY_STORE_PASSWORD=${KEYSTORE_PASSWORD} SERVER_PORT=8443 mvn spring-boot:run"
echo ""
echo "Добавить Root CA в доверенные:"
echo ""
echo "  Windows (certlm.msc):"
echo "    certlm.msc → Trusted Root Certification Authorities → All Tasks → Import → $OUTPUT_DIR/sts-root-ca.crt"
echo ""
echo "  macOS:"
echo "    sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain $OUTPUT_DIR/sts-root-ca.crt"
echo ""
echo "  Ubuntu/Debian:"
echo "    sudo cp $OUTPUT_DIR/sts-root-ca.crt /usr/local/share/ca-certificates/sts-root-ca.crt"
echo "    sudo update-ca-certificates"
