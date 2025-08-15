.PHONY: assemble clean test install release validate validate-all

# Build the plugin
assemble:
	./gradlew assemble

clean:
	rm -rf .nextflow*
	rm -rf work
	rm -rf build
	rm -rf validation/.nextflow*
	rm -rf validation/work
	rm -rf validation/results
	rm -rf validation/trace.txt
	rm -rf validation/timeline.html
	rm -rf validation/report.html
	./gradlew clean

# Run plugin unit tests
test:
	./gradlew test

integration-test:
	nextflow run validation/ -plugins nf-core-utils

# Install the plugin into local nextflow plugins dir
install:
	./gradlew install

# Publish the plugin
release:
	./gradlew releasePlugin

# Run E2E validation test
validate:
	@echo "============================================"
	@echo "nf-core-utils Plugin E2E Validation"
	@echo "============================================"
	@echo "\033[1;33m📦 Building and installing plugin...\033[0m"
	@$(MAKE) install
	@echo "\033[1;33m🧪 Running validation test...\033[0m"
	@cd validation/version-topic-channels && \
	if nextflow run . -plugins nf-core-utils@0.2.0; then \
		echo "\033[0;32m✅ Validation test passed successfully!\033[0m"; \
		if [ -f "work/pipeline_info/nf_core_utils_software_mqc_versions.yml" ]; then \
			echo "\033[0;32m✅ Output file generated correctly\033[0m"; \
			echo "\033[1;33m📄 Output file content:\033[0m"; \
			echo "---"; \
			cat work/pipeline_info/nf_core_utils_software_mqc_versions.yml; \
			echo "---"; \
		else \
			echo "\033[0;31m❌ Expected output file not found\033[0m"; \
			exit 1; \
		fi; \
		echo ""; \
		echo "\033[0;32m🎉 All validation checks passed!\033[0m"; \
		echo "\033[0;32m🚀 Plugin is ready for production use\033[0m"; \
	else \
		echo "\033[0;31m❌ Validation test failed\033[0m"; \
		exit 1; \
	fi

# Run all validation tests
validate-all:
	@echo "============================================"
	@echo "nf-core-utils Plugin Full Validation Suite"
	@echo "============================================"
	@$(MAKE) validate
