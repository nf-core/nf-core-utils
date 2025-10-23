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

# Install the plugin into local nextflow plugins dir
install:
	./gradlew install

# Publish the plugin
release:
	./gradlew releasePlugin

# Run E2E validation test
validate:
	@$(MAKE) install
	cd validation && nf-test test --verbose --debug

update-snapshots:
	@$(MAKE) install
	cd validation && nf-test test --verbose --debug --update-snapshot --clean-snapshot
