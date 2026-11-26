FROM python:3.11-slim@sha256:a5b427ace4900267d93db34138e512325c6fa6af84ad5e4ed5f3b36258cc4142
RUN pip install --no-cache-dir pymssql==2.3.11
WORKDIR /app
COPY import.py /app/import.py
ENTRYPOINT ["python", "-u", "/app/import.py"]
