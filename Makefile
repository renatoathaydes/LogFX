# Server docs: https://github.com/m3ng9i/ran

.PHONY: target
target:
	magnanimous -style doom-one

.PHONY: publish
publish:
	rm -rf target
	magnanimous -style doom-one
	git add .
	git commit -am "Publishing"
	git push

.PHONY: get-ran
get-ran:
	go install github.com/m3ng9i/ran@latest

.PHONY: run
run: target get-ran
	ran -r target

.PHONY: watch
watch: target get-ran
	ran -r target & fswatch source | (while read; do make target; done)

.PHONY: clean
clean:
	rm -rf target
