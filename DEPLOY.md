# Deploying to GitHub and Render

## 1. Push to GitHub

1. The repo owner creates an empty **public** repo (no README), e.g. `healthcare-pipeline-app`.
2. If someone else pushes, invite them under Settings, Collaborators.
3. The pusher creates a token: GitHub, Settings, Developer settings, Personal access tokens, Fine-grained tokens. Give it the repo and Contents: Read and write.
4. From this folder:

```bash
git remote add origin https://github.com/<owner>/<repo>.git
git push -u origin main
```

Username is the GitHub username; password is the token.

## 2. Deploy on Render

1. Sign up at https://render.com with GitHub and allow access to the repo.
2. New, Blueprint, choose the repo, then Apply. Render reads `render.yaml` and builds the `Dockerfile` (first build takes 5 to 10 minutes).
   Without the blueprint: New, Web Service, pick the repo, Runtime Docker, Instance type Free, Health check path `/`.
3. When the build finishes the service shows a link like `https://healthcare-pipeline.onrender.com`.

## 3. Check it

- Open the link: Book a visit shows "Appointment booked".
- Concurrency test: 30 requests, 1 confirmed.
- Lab results: run the batch; expected 303 read, 300 saved, 5 skipped.
- `/ws/insurance.wsdl` serves the SOAP contract.

## Notes for the free plan

- The service sleeps after about 15 minutes idle; the first visit then takes about a minute. Open it a few minutes before presenting.
- Data lives in memory and resets on every restart or wake-up; doctors and slots are re-seeded automatically.
- Memory is capped for the 512 MB limit (see `JAVA_TOOL_OPTIONS` in the Dockerfile). Measured peak locally: about 306 MB.
- `/h2-console` is enabled for demos. Turn it off for anything public and permanent
  (`spring.h2.console.enabled=false`).

## Logins

`password.txt` holds the demo logins and is copied into the Docker image. Anyone who can read the public repo can read these passwords, so treat the deployed demo as open. For anything beyond a class demo, change them and keep the file out of the repo.
