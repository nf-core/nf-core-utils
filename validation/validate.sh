#!/bin/bash

set -euo pipefail

echo "============================================"
echo "nf-core-utils Plugin E2E Validation Script"
echo "============================================"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if nextflow is available
if ! command -v nextflow &> /dev/null; then
    echo -e "${RED}❌ Nextflow is not installed or not in PATH${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Nextflow found: $(nextflow -version | head -n1)${NC}"

# Build and install plugin
echo -e "${YELLOW}📦 Building and installing plugin...${NC}"
cd "$(dirname "$0")/.."
make install

echo -e "${YELLOW}🧪 Running validation test...${NC}"
cd validation

# Run the test
if nextflow run . -plugins nf-core-utils@0.2.0; then
    echo -e "${GREEN}✅ Validation test passed successfully!${NC}"
    
    # Check for expected output files
    if [ -f "work/pipeline_info/nf_core_utils_software_mqc_versions.yml" ]; then
        echo -e "${GREEN}✅ Output file generated correctly${NC}"
        echo -e "${YELLOW}📄 Output file content:${NC}"
        echo "---"
        cat work/pipeline_info/nf_core_utils_software_mqc_versions.yml
        echo "---"
    else
        echo -e "${RED}❌ Expected output file not found${NC}"
        exit 1
    fi
    
    echo ""
    echo -e "${GREEN}🎉 All validation checks passed!${NC}"
    echo -e "${GREEN}🚀 Plugin is ready for production use${NC}"
    
else
    echo -e "${RED}❌ Validation test failed${NC}"
    exit 1
fi