# Security Policy

We take security faults seriously, but as this code is designed to be used as
a free-standing executable called from Python code with full user permissions
(including the ability to run arbitrary programs if desired), serious security
problems are expected to be minimal; there's no privilege separation in the
first place by design.

Security issues with an impact on
[Spalloc Server](https://github.com/SpiNNakerManchester/JavaSpiNNaker/tree/master/SpiNNaker-allocserv)
are not expected to have wide impact; there are not expected to be many
deployments of that service. However, it is one of the more security-exposed
parts of the stack.

Other SpiNNaker software may have its own security policies.

## Supported Versions

Currently, we only support the most recent release or the git `master` branch.
If a security problem can be resolved by simply installing and building the
current version of the code, that will _consistently_ be our answer.

| Version | Supported          |
| ------- | ------------------ |
| git `master` branch | :white_check_mark: |
| 6.0     | :white_check_mark: |
| 5.1     | :x:                |
| 5.0     | :x:                |
| 4.0.    | :x:                |
| < 4.0   | :x:                |

## Reporting a Vulnerability

Please clearly put **SECURITY:** at the start of the title of communications on
security issues, whether using issues or email.

If you find a problem and don't wish to just
[report an Issue](https://github.com/SpiNNakerManchester/JavaSpiNNaker/issues/new) or
[make a Pull Request](https://github.com/SpiNNakerManchester/JavaSpiNNaker/compare)
due to confidentiality concerns, you may send an email to the
[SpiNNaker team mailing list](mailto:SPINNAKER@listserv.manchester.ac.uk) (moderated,
non-public, distribution not very small but includes all people who need to know so
that expedited action can be taken).

Contacting individual developers is not recommended; it is too easy for messages to
get lost like that.
