"""Domain errors, translated to HTTP responses only in the API layer."""


class DomainError(Exception):
    pass


class AuthenticationFailed(DomainError):
    pass


class DatabaseUnavailable(DomainError):
    pass


class DatabaseOperationFailed(DomainError):
    pass


class SourceNotFound(DomainError):
    pass


class TableNotFound(DomainError):
    pass
