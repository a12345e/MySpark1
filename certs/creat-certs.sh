BASE_DIR="/c/create-certs"
CERT_DIR="$BASE_DIR/certs"

# Color codes
CYAN='\033[0;36m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${CYAN}Checking cert directory...${NC}"
mkdir -p "$CERT_DIR"

# Convert the Bash path to a Windows path for Docker volumes
# Example: /c/dockers/... -> C:\dockers\...
WIN_CERT_DIR=$(cygpath -w "$CERT_DIR")

# ------------------------------
# Step 1: Generate Certificates (Skip if exist)
# ------------------------------
if [ -f "$CERT_DIR/elastic-stack-ca.p12" ]; then
    echo -e "${YELLOW}REPORT: CA already exists. Skipping generation.${NC}"
else
    echo -e "${CYAN}Generating CA and Node certificates via Docker...${NC}"

    # Generate CA using the dynamic Windows path
    docker run --rm -v "${WIN_CERT_DIR}:/certs" \
      docker.elastic.co/elasticsearch/elasticsearch:8.13.4 \
      bash -c "elasticsearch-certutil ca --silent --out /certs/elastic-stack-ca.p12 --pass ''"

    # Generate Node Cert using the dynamic Windows path
    docker run --rm -v "${WIN_CERT_DIR}:/certs" \
      docker.elastic.co/elasticsearch/elasticsearch:8.13.4 \
      bash -c "elasticsearch-certutil cert --silent --ca /certs/elastic-stack-ca.p12 --ca-pass '' --out /certs/elastic-certificates.p12 --pass ''"
fi

# ------------------------------
# Step 3: Extract and Clean CA
# ------------------------------
echo -e "${CYAN}Step 3: Extracting and Cleaning CA...${NC}"
cd "$CERT_DIR" || exit

# Extracting using the verified redirection method
openssl pkcs12 -in elastic-stack-ca.p12 -nokeys -info -passin pass:"" > ca.crt 2>/dev/null

if [ -s ca.crt ]; then
    # Your requested clean-up command
    openssl x509 -in ca.crt -out clean-ca.crt
    echo -e "${GREEN}Created clean-ca.crt successfully.${NC}"
else
    echo -e "${RED}FATAL: ca.crt is empty. Extraction failed.${NC}"
    exit 1
fi

# ------------------------------
# Step 4: Import into Java Keystore (JKS)
# ------------------------------
echo -e "${CYAN}Step 4: Finalizing Java Truststore...${NC}"

if [ -f "elastic-truststore.jks" ] && keytool -list -keystore elastic-truststore.jks -storepass mypassword -alias elasticsearch > /dev/null 2>&1; then
    echo -e "${YELLOW}REPORT: Alias 'elasticsearch' already exists in JKS.${NC}"
else
    # Your requested import command
    keytool -import -file clean-ca.crt -alias elasticsearch -keystore elastic-truststore.jks -storepass mypassword -noprompt
    echo -e "${GREEN}SUCCESS: elastic-truststore.jks is ready.${NC}"
fi

# ------------------------------
# Step 5: Fix Windows permissions
# ------------------------------
echo -e "${CYAN}Fixing permissions...${NC}"
icacls "$WIN_CERT_DIR" /grant Everyone:F /T > /dev/null

echo -e "\n${GREEN}COMPLETE! Certificates are in: $CERT_DIR${NC}"
ls -lh