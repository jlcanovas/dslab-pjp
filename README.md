# DSLab GitHub Action for PJP

## 📚 Description

GitHub Action for submitting assignment to DSLab tool. The current implementation has been tailored to be used in the subject called [Programación JavaScript para Programadores](https://github.com/PJP-UOC).

It currently performs the following steps:

1. Checkouts this repo and compiles the code.
2. Checkouts the repo from where the action has been triggered and zips this repo.
3. Submits the zip file to DSLab.
4. Remove the zip file.

## 🚀 Usage

This GitHub action is intended to be used in a workflow triggered by a push event. It will submit the assignment to the DSLab tool.

The action requires the following parameters:

- `student`: the student email, who must be registered in DSLab.
- `tenant`: the DSLab tenant name.
- `project`: the project name in DSLab.
- `user`: the DSLab user with permissions to do the submission.
- `pass`: the DSLab user password.

`tenant`, `project`, `user` and `pass` are expected to be stored as secrets in the repository or organization.

### Example

The following code shows an example of a workflow that uses this action:

```yaml
name: dslab
on:
  push
jobs:
  submit-to-dslab:
    name: Submits project to DSLab
    runs-on: ubuntu-latest
    steps:
      - name: submits to dslab
        uses: jlcanovas/dslab-pjp@v1
        with:
          student: "email@student.edu"
          tenant: ${{ secrets.DSLAB_TENANT }}
          project: ${{ secrets.DSLAB_PROJECT }}
          user: ${{ secrets.DSLAB_USER }}
          pass: ${{ secrets.DSLAB_PASS }}
```

## ⚖️ License

This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/4.0/">Creative Commons Attribution 4.0 International License</a>

The [CC BY](https://creativecommons.org/licenses/by-sa/4.0/) license allows reusers to distribute, remix, adapt, and build upon the material in any medium or format, so long as attribution is given to the creator. The license allows for commercial use. 

<a rel="license" href="http://creativecommons.org/licenses/by-sa/4.0/"><img alt="Creative Commons License" style="border-width:0" src="https://i.creativecommons.org/l/by-sa/4.0/88x31.png" /></a>


