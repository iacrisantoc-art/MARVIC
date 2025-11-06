{ pkgs, ... }: {
  # Which nixpkgs channel to use.
  channel = "stable-24.05"; # or "unstable"

  # Use https://search.nixos.org/packages to find packages
  packages = [
    # pkgs.go
    # pkgs.python311
    # pkgs.python311Packages.pip
    # pkgs.nodejs_20
  ];

  # Sets environment variables in the workspace
  env = {
    # GEMINI_API_KEY = "";
  };

  # Services exposed on ports from the workspace
  services.ports = [
    # { port = 8080; }
  ];

  # Defines a script to run when the workspace starts
  start.prebuild = ''
    # echo "Hello, World!"
  '';
}
