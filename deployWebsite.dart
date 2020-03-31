import 'dart:io' show exit, Directory;

import 'checkoutWebsite.dart';

main(List<String> args) async {
  final siteSource = 'website';
  final siteBranch = 'gh-pages';
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
  await generateSite(source: siteSource, destination: siteBranch);
  print('Done!');
}

Future<void> generateSite({String source, String destination}) async {
  print("Removing old publication.");
  await Directory(destination).recreateEmpty();
  print("Generating website.");
  await ['magnanimous', '-globalctx=_github_global_context'].execute(wrkDir: source);
  print("Moving website to destination: $destination.");

  await for (final entry in Directory("$source/target").list()) {
    await entry.rename(
        "$destination/${entry.path.substring(entry.path.lastIndexOf('/'))}");
  }
}
