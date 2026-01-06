.PHONY: build-HealthcheckFunction

build-HealthcheckFunction:
	cp target/main.js $(ARTIFACTS_DIR)/main.js
	cp package.json $(ARTIFACTS_DIR)/
	cp package-lock.json $(ARTIFACTS_DIR)/
	cd $(ARTIFACTS_DIR) && npm ci --production
