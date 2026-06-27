package yowyob.comops.api.roles.adapter.in.web;

import yowyob.comops.api.common.domain.model.ApiResponse;
import yowyob.comops.api.kernel.application.service.ReactiveRequestContextHolder;
import yowyob.comops.api.roles.application.port.in.AssignRoleToUserCommand;
import yowyob.comops.api.roles.application.port.in.AssignRoleToUserUseCase;
import yowyob.comops.api.roles.application.port.in.CreateRoleCommand;
import yowyob.comops.api.roles.application.port.in.CreateRoleUseCase;
import yowyob.comops.api.roles.application.port.in.DeleteRoleUseCase;
import yowyob.comops.api.roles.application.port.in.GetRoleUseCase;
import yowyob.comops.api.roles.application.port.in.ListRolesUseCase;
import yowyob.comops.api.roles.application.port.in.ListUserRoleAssignmentsUseCase;
import yowyob.comops.api.roles.application.port.in.RevokeRoleAssignmentUseCase;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@Validated
@RequestMapping("/api/roles")
@PreAuthorize("@businessAccessPolicy.canManageIdentity(authentication)")
public class RoleController {

    private final CreateRoleUseCase createRoleUseCase;
    private final AssignRoleToUserUseCase assignRoleToUserUseCase;
    private final ListRolesUseCase listRolesUseCase;
    private final GetRoleUseCase getRoleUseCase;
    private final DeleteRoleUseCase deleteRoleUseCase;
    private final ListUserRoleAssignmentsUseCase listUserRoleAssignmentsUseCase;
    private final RevokeRoleAssignmentUseCase revokeRoleAssignmentUseCase;

    public RoleController(CreateRoleUseCase createRoleUseCase,
            AssignRoleToUserUseCase assignRoleToUserUseCase,
            ListRolesUseCase listRolesUseCase,
            GetRoleUseCase getRoleUseCase,
            DeleteRoleUseCase deleteRoleUseCase,
            ListUserRoleAssignmentsUseCase listUserRoleAssignmentsUseCase,
            RevokeRoleAssignmentUseCase revokeRoleAssignmentUseCase) {
        this.createRoleUseCase = createRoleUseCase;
        this.assignRoleToUserUseCase = assignRoleToUserUseCase;
        this.listRolesUseCase = listRolesUseCase;
        this.getRoleUseCase = getRoleUseCase;
        this.deleteRoleUseCase = deleteRoleUseCase;
        this.listUserRoleAssignmentsUseCase = listUserRoleAssignmentsUseCase;
        this.revokeRoleAssignmentUseCase = revokeRoleAssignmentUseCase;
    }

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<RoleResponse>>> createRole(@Valid @RequestBody Mono<CreateRoleRequest> requestMono) {
        return requestMono
                .zipWith(ReactiveRequestContextHolder.getRequiredContext())
                .flatMap(tuple -> createRoleUseCase.createRole(new CreateRoleCommand(
                        tuple.getT2().tenantId(),
                        tuple.getT1().code(),
                        tuple.getT1().name(),
                        tuple.getT1().scopeType(),
                        tuple.getT1().permissions())))
                .map(RoleResponse::from)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success(response, "Role created.")));
    }

    @GetMapping
    public Flux<RoleResponse> listRoles() {
        return ReactiveRequestContextHolder.getRequiredContext()
                .flatMapMany(ctx -> listRolesUseCase.listRoles(ctx.tenantId()))
                .map(RoleResponse::from);
    }

    @GetMapping("/{id}")
    public Mono<RoleResponse> getRole(@PathVariable UUID id) {
        return ReactiveRequestContextHolder.getRequiredContext()
                .flatMap(ctx -> getRoleUseCase.getRole(ctx.tenantId(), id))
                .map(RoleResponse::from);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteRole(@PathVariable UUID id) {
        return ReactiveRequestContextHolder.getRequiredContext()
                .flatMap(ctx -> deleteRoleUseCase.deleteRole(ctx.tenantId(), id));
    }

    @PostMapping("/assignments")
    public Mono<ResponseEntity<ApiResponse<UserRoleAssignmentResponse>>> assignRole(@Valid @RequestBody Mono<AssignRoleToUserRequest> requestMono) {
        return requestMono
                .zipWith(ReactiveRequestContextHolder.getRequiredContext())
                .flatMap(tuple -> assignRoleToUserUseCase.assign(new AssignRoleToUserCommand(
                        tuple.getT2().tenantId(),
                        tuple.getT1().userId(),
                        tuple.getT1().roleId(),
                        tuple.getT1().scopeType(),
                        tuple.getT1().scopeId(),
                        tuple.getT1().scope())))
                .map(UserRoleAssignmentResponse::from)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success(response, "Role assigned.")));
    }

    @GetMapping("/assignments")
    public Flux<UserRoleAssignmentResponse> listAssignments(@RequestParam UUID userId) {
        return ReactiveRequestContextHolder.getRequiredContext()
                .flatMapMany(ctx -> listUserRoleAssignmentsUseCase.listAssignments(ctx.tenantId(), userId))
                .map(UserRoleAssignmentResponse::from);
    }

    @DeleteMapping("/assignments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> revokeAssignment(@PathVariable UUID id) {
        return ReactiveRequestContextHolder.getRequiredContext()
                .flatMap(ctx -> revokeRoleAssignmentUseCase.revoke(ctx.tenantId(), id));
    }
}
