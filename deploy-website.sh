#!/bin/bash

DIR=$(dirname "$0")
TARGET=$DIR/website/target

git diff-index --quiet HEAD

if [ $? != 0 ]
then
    echo "Git worktree is dirty. Commit or stash changes before proceeding."
    exit 1;
fi

echo "Deleting old publication"
rm -rf $TARGET
mkdir $TARGET
git worktree prune
rm -rf .git/worktrees/website

echo "Checking out gh-pages branch into $TARGET"
git worktree add -B gh-pages $TARGET origin/gh-pages

echo "Generating website"
magnanimous -style doom-one -globalctx _github_global_context website

if [[ "$@" == *"-push"* ]]; then
    echo "Updating gh-pages branch"
    cd $TARGET && git add --all && git commit -m "Publishing website" && git push
else
    echo "Skipping push to gh-pages branch (use -push to enable)"
fi
