import 'dart:io'
    show exit, FileSystemEntity, FileSystemEntityType, File, Directory;

import 'checkoutWebsite.dart';

main(List<String> args) async {
  final siteSource = 'website';
  final ghPagesBranchAndDir = 'gh-pages';
  if (!(await branchExists(ghPagesBranchAndDir))) {
    if (args.length == 1 && args[0] == '-f') {
      await createEmptyBranch(ghPagesBranchAndDir);
    } else {
      print(
          '$ghPagesBranchAndDir branch does not exist. Run with -f to create it.');
      exit(1);
    }
  }
  await cleanup(branch: ghPagesBranchAndDir, dir: ghPagesBranchAndDir);
  await checkout(branch: ghPagesBranchAndDir, dir: ghPagesBranchAndDir);
  await generateSite(source: siteSource, destination: ghPagesBranchAndDir);
  print('Done!');
}

Future<void> generateSite(
    {required String source, required String destination}) async {
  var destDir = Directory(destination);
  print("Generating website with Magnanimous.");
  await ['magnanimous', '-globalctx=_github_global_context']
      .execute(wrkDir: source);
  print("Moving website to destination: $destDir.");

  await for (final entry in Directory("$source/target").list()) {
    final newName =
        "${destDir.path}${entry.path.substring(entry.path.lastIndexOf('/'))}";
    await removeIfExists(newName);
    await entry.rename(newName);
  }
}

Future<void> removeIfExists(String path) async {
  final type = await FileSystemEntity.type(path);
  if (type == FileSystemEntityType.file) {
    await File(path).delete();
  } else if (type == FileSystemEntityType.directory) {
    await Directory(path).delete(recursive: true);
  }
}
