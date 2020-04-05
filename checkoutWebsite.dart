import 'dart:io';

main(List<String> args) async {
  final siteBranch = 'website';
  if (!(await branchExists(siteBranch))) {
    if (args.length == 1 && args[0] == '-f') {
      await createEmptyBranch(siteBranch);
    } else {
      print('$siteBranch branch does not exist. Run with -f to create it.');
      exit(1);
    }
  }
  await cleanup(branch: siteBranch, dir: siteBranch);
  await checkout(branch: siteBranch, dir: siteBranch);
  print('Done!');
}

Future<void> cleanup({String branch, String dir}) async {
  print('Cleaning up directory "$dir" and worktree!');
  await Directory(dir).recreateEmpty();
  ['git', 'worktree', 'prune'].execute();
  final workTreeDir = Directory('.git/worktrees/$dir');
  if ((await workTreeDir.exists())) {
    await workTreeDir.delete(recursive: true);
  }
}

Future<void> checkout({String branch, String dir}) async {
  print('Checking out branch "$branch" into directory "$dir"!');
  await ['git', 'worktree', 'add', '-B', branch, dir, 'origin/$branch'].execute();
}

Future<String> currentBranch() {
  return ['git', 'rev-parse', '--abbrev-ref', 'HEAD']
      .execute()
      .then((p) => p.stdout.toString().trim());
}

Future<bool> branchExists(String name) async {
  final res = (await ['git', 'branch', '--list', name].execute());
  return RegExp('([+\\-*]\\s+)?$name').hasMatch(res.stdout.toString().trim());
}

Future<void> createEmptyBranch(String name) async {
  final branch = await currentBranch();
  print('Creating empty branch with name "$name"');
  await ['git', 'checkout', '--orphan', name].execute();
  await ['git', 'reset', '--hard'].execute();
  await ['git', 'commit', '--allow-empty', '-m', 'Initializing gh-pages branch']
      .execute();
  print('Pushing branch "$name"');
  await ['git', 'push', 'origin', name].execute();
  await ['git', 'checkout', branch].execute();
}

extension Exec on List<String> {
  Future<ProcessResult> execute(
      {bool checkStatus = true, String wrkDir}) async {
    final res = await Process.run(this.first, this.skip(1).toList(),
        runInShell: true, workingDirectory: wrkDir);
    if (checkStatus && res.exitCode != 0) {
      stderr.writeln('Command exited with ${res.exitCode}: $this');
      print(res.stdout);
      stderr.writeln(res.stderr);
      exit(res.exitCode);
    }
    return res;
  }
}

extension DirExt on Directory {
  Future<void> recreateEmpty() async {
    final abs = this.absolute;
    if (await abs.exists()) {
      print("Deleting $abs recursively");
      await abs.delete(recursive: true);
    }
    await abs.create();
  }
}
