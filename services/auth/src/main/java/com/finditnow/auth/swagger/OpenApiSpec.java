package com.finditnow.auth.swagger;

public final class OpenApiSpec {

    private OpenApiSpec() {}

    public static final String JSON = """
{
  "openapi": "3.0.1",
  "info": {
    "title": "FindItNow Auth Service",
    "version": "1.0.0",
    "description": "Authentication & Authorization APIs"
  },
  "servers": [
    {
      "url": "http://localhost:9000/auth"
    }
  ],
  "paths": {

    "/signup": {
      "post": {
        "summary": "Sign up user",
        "requestBody": {
          "required": true,
          "content": {
            "application/json": {
              "example": {
                "email": "user@gmail.com",
                "password": "Strong@123",
                "role": "customer"
              }
            }
          }
        },
        "responses": {
          "201": { "description": "User registered" }
        }
      }
    },

    "/signin": {
      "post": {
        "summary": "Sign in",
        "requestBody": {
          "required": true,
          "content": {
            "application/json": {
              "example": {
                "email": "user@gmail.com",
                "password": "Strong@123"
              }
            }
          }
        },
        "responses": {
          "200": { "description": "Login successful" }
        }
      }
    },

    "/verifyemail": {
      "post": {
        "summary": "Verify email",
        "requestBody": {
          "required": true,
          "content": {
            "application/json": {
              "example": {
                "credId": "uuid-here",
                "verificationCode": "123456"
              }
            }
          }
        },
        "responses": {
          "200": { "description": "Email verified" }
        }
      }
    },

    "/resendverificationemail": {
      "post": {
        "summary": "Resend verification email",
        "requestBody": {
          "required": true,
          "content": {
            "application/json": {
              "example": {
                "credId": "uuid-here"
              }
            }
          }
        },
        "responses": {
          "200": { "description": "Verification email sent" }
        }
      }
    },

    "/sendresettoken": {
      "post": {
        "summary": "Send password reset token",
        "requestBody": {
          "required": true,
          "content": {
            "application/json": {
              "example": {
                "email": "user@gmail.com"
              }
            }
          }
        },
        "responses": {
          "200": { "description": "Reset token sent" }
        }
      }
    },

    "/verifyresettoken": {
      "get": {
        "summary": "Verify reset token",
        "parameters": [
          {
            "name": "resetToken",
            "in": "query",
            "required": true,
            "schema": { "type": "string" }
          }
        ],
        "responses": {
          "200": { "description": "Token verified" }
        }
      }
    },

    "/resetpassword": {
      "put": {
        "summary": "Reset password",
        "requestBody": {
          "required": true,
          "content": {
            "application/json": {
              "example": {
                "email": "user@gmail.com",
                "newPassword": "NewStrong@123"
              }
            }
          }
        },
        "responses": {
          "200": { "description": "Password reset successful" }
        }
      }
    },

    "/updatepassword": {
      "put": {
        "summary": "Update password (logged-in)",
        "security": [{ "bearerAuth": [] }],
        "requestBody": {
          "required": true,
          "content": {
            "application/json": {
              "example": {
                "newPassword": "Updated@123"
              }
            }
          }
        },
        "responses": {
          "200": { "description": "Password updated" }
        }
      }
    },

    "/updaterole": {
      "put": {
        "summary": "Update role",
        "security": [{ "bearerAuth": [] }],
        "requestBody": {
          "required": true,
          "content": {
            "application/json": {
              "example": {
                "role": "OWNER"
              }
            }
          }
        },
        "responses": {
          "200": { "description": "Role updated" }
        }
      }
    },

    "/logout": {
      "post": {
        "summary": "Logout",
        "security": [{ "bearerAuth": [] }],
        "responses": {
          "200": { "description": "Logged out" }
        }
      }
    },

    "/oauth/google": {
      "get": {
        "summary": "Google OAuth redirect",
        "responses": {
          "302": { "description": "Redirect to Google" }
        }
      }
    },

    "/health": {
      "get": {
        "summary": "Health check",
        "responses": {
          "200": { "description": "OK" }
        }
      }
    }
  },

  "components": {
    "securitySchemes": {
      "bearerAuth": {
        "type": "http",
        "scheme": "bearer",
        "bearerFormat": "JWT"
      }
    }
  }
}
""";
}
